package com.sanya.client.service.files;

import com.sanya.events.core.EventBus;
import com.sanya.files.FileChunk;
import com.sanya.files.FileTransferEvent;
import com.sanya.files.FileTransferRequest;

import java.io.File;
import java.io.FileInputStream;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FileSender {

    private static final Logger log = Logger.getLogger(FileSender.class.getName());

    public static void sendFile(File file, String username,
                                Consumer<Object> sender, EventBus eventBus) throws Exception {

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

                int percent = (int) ((100.0 * sentBytes) / totalBytes);
                eventBus.publish(new FileTransferEvent(FileTransferEvent.Type.PROGRESS, file.getName(), sentBytes, totalBytes, true, null));
                log.fine("Progress " + percent + "% (" + sentBytes + "/" + totalBytes + ")");
            }

            eventBus.publish(new FileTransferEvent(FileTransferEvent.Type.COMPLETED, file.getName(), totalBytes, totalBytes, true, null));
            log.info("File transfer completed: " + file.getName());
        } catch (Exception e) {
            eventBus.publish(new FileTransferEvent(FileTransferEvent.Type.FAILED, file.getName(), sentBytes, totalBytes, true, e.getMessage()));
            log.log(Level.SEVERE, "File transfer failed: " + file.getName(), e);
            throw e;
        }
    }
}
