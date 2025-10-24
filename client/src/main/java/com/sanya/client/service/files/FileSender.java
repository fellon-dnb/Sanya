package com.sanya.client.service.files;

import com.sanya.client.core.api.EventBus;
import com.sanya.client.core.api.FileTransferService;
import com.sanya.events.core.DefaultEventBus;
import com.sanya.files.FileChunk;
import com.sanya.files.FileTransferEvent;
import com.sanya.files.FileTransferRequest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FileSender implements FileTransferService {

    private static final Logger log = Logger.getLogger(FileSender.class.getName());
    private final EventBus eventBus;
    private final Map<String, FileReceiverState> receivers = new HashMap<>();
    private final Map<String, FileOutputStream> openFiles = new HashMap<>();

    public FileSender(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    public void sendFile(String username, File file, Consumer<Object> sender)  {
        long totalBytes = file.length();
        long sentBytes = 0;
        int bufferSize = 8192;
        byte[] buffer = new byte[bufferSize];

        sender.accept(new FileTransferRequest(username, file.getName(), totalBytes));
        eventBus.publish(new FileTransferEvent(FileTransferEvent.Type.STARTED, file.getName(), 0, totalBytes, true, null));
        log.info("Started sending file: " + file.getName() + " (" + totalBytes + " bytes)");

        try (FileInputStream fis = new FileInputStream(file)) {
            int part = 0;
            int read;
            while ((read = fis.read(buffer)) != -1) {
                boolean last = (sentBytes + read) >= totalBytes;
                FileChunk chunk = new FileChunk(file.getName(), buffer.clone(), part++, last);
                sender.accept(chunk);
                sentBytes += read;

                eventBus.publish(new FileTransferEvent(FileTransferEvent.Type.PROGRESS, file.getName(), sentBytes, totalBytes, true, null));
            }

            eventBus.publish(new FileTransferEvent(FileTransferEvent.Type.COMPLETED, file.getName(), totalBytes, totalBytes, true, null));
            log.info("File transfer completed: " + file.getName());
        } catch (Exception e) {
            eventBus.publish(new FileTransferEvent(FileTransferEvent.Type.FAILED, file.getName(), sentBytes, totalBytes, true, e.getMessage()));
            log.log(Level.SEVERE, "File transfer failed: " + file.getName(), e);

        }
    }

    @Override
    public void receiveFile(FileChunk chunk) {
        try {
            FileOutputStream out = openFiles.computeIfAbsent(
                    chunk.getFilename(),
                    name -> {
                        try {
                            return new FileOutputStream("recv_" + name, true);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
            );

            out.write(chunk.getData());
            out.flush();

            if (chunk.isLast()) {
                out.close();
                openFiles.remove(chunk.getFilename());
                eventBus.publish(new FileTransferEvent(FileTransferEvent.Type.COMPLETED, chunk.getFilename(), -1, -1, false, null));
            }
        } catch (Exception e) {
            eventBus.publish(new FileTransferEvent(FileTransferEvent.Type.FAILED, chunk.getFilename(), -1, -1, false, e.getMessage()));
            log.log(Level.WARNING, "File receive failed: " + chunk.getFilename(), e);
        }
    }

    private static class FileReceiverState {
        final FileOutputStream out;

        FileReceiverState(String fileName) throws Exception {
            File target = new File("recv_" + fileName);
            this.out = new FileOutputStream(target, true);
        }
    }
}
