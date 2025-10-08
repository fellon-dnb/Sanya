package com.sanya.client.files;

import com.sanya.events.EventBus;
import com.sanya.files.FileChunk;
import com.sanya.files.FileTransferEvent;
import com.sanya.files.FileTransferRequest;

import java.io.*;

public class FileSender {

    public static void sendFile(File file, String username, ObjectOutputStream out, EventBus eventBus) throws IOException {
        long totalBytes = file.length();
        long sentBytes = 0;
        int bufferSize = 8192; // 8 KB
        byte[] buffer = new byte[bufferSize];

        // уведомляем сервер о начале передачи
        FileTransferRequest request = new FileTransferRequest(username, file.getName(), totalBytes);
        out.writeObject(request);
        out.flush();

        eventBus.publish(new FileTransferEvent(
                FileTransferEvent.Type.STARTED,
                file.getName(),
                0,
                totalBytes,
                true,
                null
        ));

        try (FileInputStream fis = new FileInputStream(file)) {
            int part = 0;
            int read;
            while ((read = fis.read(buffer)) != -1) {
                boolean last = (sentBytes + read) >= totalBytes;
                FileChunk chunk = new FileChunk(file.getName(), buffer.clone(), part++, last);
                out.writeObject(chunk);
                out.flush();
                sentBytes += read;

                eventBus.publish(new FileTransferEvent(
                        FileTransferEvent.Type.PROGRESS,
                        file.getName(),
                        sentBytes,
                        totalBytes,
                        true,
                        null
                ));
            }

            eventBus.publish(new FileTransferEvent(
                    FileTransferEvent.Type.COMPLETED,
                    file.getName(),
                    totalBytes,
                    totalBytes,
                    true,
                    null
            ));

        } catch (Exception e) {
            eventBus.publish(new FileTransferEvent(
                    FileTransferEvent.Type.FAILED,
                    file.getName(),
                    sentBytes,
                    totalBytes,
                    true,
                    e.getMessage()
            ));
            throw e;
        }
    }
}
