package com.sanya.client.service.files;

import com.sanya.events.core.EventBus;
import com.sanya.events.file.FileIncomingEvent;
import com.sanya.files.FileChunk;
import com.sanya.files.FileTransferEvent;
import com.sanya.files.FileTransferRequest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;

/**
 * FileReceiverThread — поток для приёма файлов от сервера.
 * Работает в фоне, уведомляет EventBus о прогрессе и завершении.
 */
public class FileReceiverThread extends Thread {

    private final ObjectInputStream in;
    private final File saveFile;
    private final EventBus eventBus;
    private final FileTransferRequest request;

    public FileReceiverThread(FileIncomingEvent event, File saveFile, EventBus eventBus) {
        super("FileReceiver-" + saveFile.getName());
        this.in = event.input();
        this.saveFile = saveFile;
        this.eventBus = eventBus;
        this.request = event.request();
    }

    @Override
    public void run() {
        long total = request.getSize();
        long received = 0;

        eventBus.publish(new FileTransferEvent(
                FileTransferEvent.Type.STARTED,
                saveFile.getName(), 0, total, false, "Receiving from " + request.getSender()
        ));

        try (FileOutputStream fos = new FileOutputStream(saveFile)) {
            while (true) {
                Object obj = in.readObject();
                if (!(obj instanceof FileChunk chunk)) break;

                fos.write(chunk.getData());
                received += chunk.getData().length;

                eventBus.publish(new FileTransferEvent(
                        FileTransferEvent.Type.PROGRESS,
                        saveFile.getName(), received, total, false, null
                ));

                if (chunk.isLast()) break;
            }

            eventBus.publish(new FileTransferEvent(
                    FileTransferEvent.Type.COMPLETED,
                    saveFile.getName(), total, total, false, null
            ));

        } catch (Exception e) {
            eventBus.publish(new FileTransferEvent(
                    FileTransferEvent.Type.FAILED,
                    saveFile.getName(), 0, total, false, e.getMessage()
            ));
        }
    }
}
