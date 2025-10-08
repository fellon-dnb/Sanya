package com.sanya.files;

import java.io.Serializable;

public class FileTransferRequest implements Serializable {
    private final String filename;
    private final long size;
    private final String sender;

    public FileTransferRequest(String sender, String filename, long size) {
        this.sender = sender;
        this.filename = filename;
        this.size = size;
    }

    public String getFilename() { return filename; }
    public long getSize() { return size; }
    public String getSender() { return sender; }

    @Override
    public String toString() {
        return "[FileRequest] " + sender + " → " + filename + " (" + size + " bytes)";
    }
}
