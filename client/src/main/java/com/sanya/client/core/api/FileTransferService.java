package com.sanya.client.core.api;

import com.sanya.files.FileChunk;

import java.io.File;
import java.util.function.Consumer;

public interface FileTransferService {
    void sendFile(String recipient, File file, Consumer<Object> sender);
    void receiveFile(FileChunk chunk);
}
