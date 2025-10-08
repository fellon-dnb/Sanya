package com.sanya.client;

import com.sanya.events.EventBus;
import com.sanya.files.FileChunk;
import com.sanya.files.FileTransferEvent;
import com.sanya.files.FileTransferRequest;

import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.File;
@Deprecated
public class FileReceiver {

    public static void receiveFile(ObjectInputStream in, String saveDir, EventBus eventBus) throws Exception {
        FileTransferRequest request = (FileTransferRequest) in.readObject();
        File file = new File(saveDir, request.getFilename());
        long total = request.getSize();
        long received = 0;

        eventBus.publish(new FileTransferEvent(FileTransferEvent.Type.STARTED, file.getName(), 0, total, false, null));

        try (FileOutputStream fos = new FileOutputStream(file)) {
            while (true) {
                FileChunk chunk = (FileChunk) in.readObject();
                fos.write(chunk.getData());
                received += chunk.getData().length;

                eventBus.publish(new FileTransferEvent(FileTransferEvent.Type.PROGRESS,
                        file.getName(), received, total, false, null));

                if (chunk.isLast()) break;
            }
        }

        eventBus.publish(new FileTransferEvent(FileTransferEvent.Type.COMPLETED,
                file.getName(), received, total, false, null));
    }
}
