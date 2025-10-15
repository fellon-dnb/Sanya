package com.sanya.client.files;

import com.sanya.events.EventBus;
import com.sanya.files.FileChunk;
import com.sanya.files.FileTransferEvent;
import com.sanya.files.FileTransferRequest;

import java.io.File;
import java.io.FileInputStream;
import java.util.function.Consumer;

public class FileSender {

    public static void sendFile(File file, String username,
                                Consumer<Object> sender, EventBus eventBus) throws Exception {

        long totalBytes = file.length();
        long sentBytes = 0;
        int bufferSize = 8192;
        byte[] buffer = new byte[bufferSize];

        sender.accept(new FileTransferRequest(username, file.getName(), totalBytes));

        eventBus.publish(new FileTransferEvent(
                FileTransferEvent.Type.STARTED, file.getName(), 0, totalBytes, true, null
        ));

        try (FileInputStream fis = new FileInputStream(file)) {
            int part = 0;
            int read;
            while ((read = fis.read(buffer)) != -1) {
                boolean last = (sentBytes + read) >= totalBytes;
                FileChunk chunk = new FileChunk(file.getName(), buffer.clone(), part++, last);
                sender.accept(chunk);
                sentBytes += read;

                eventBus.publish(new FileTransferEvent(
                        FileTransferEvent.Type.PROGRESS, file.getName(), sentBytes, totalBytes, true, null
                ));
            }

            eventBus.publish(new FileTransferEvent(
                    FileTransferEvent.Type.COMPLETED, file.getName(), totalBytes, totalBytes, true, null
            ));

        } catch (Exception e) {
            eventBus.publish(new FileTransferEvent(
                    FileTransferEvent.Type.FAILED, file.getName(), sentBytes, totalBytes, true, e.getMessage()
            ));
            throw e;
        }
    }
}
