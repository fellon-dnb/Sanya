package com.sanya.files;

import java.io.Serializable;

public class FileChunk implements Serializable {
    private final String filename;
    private final byte[] data;
    private final int part;
    private final boolean last;

    public FileChunk(String filename, byte[] data, int part, boolean last) {
        this.filename = filename;
        this.data = data;
        this.part = part;
        this.last = last;
    }

    public String getFilename() { return filename; }
    public byte[] getData() { return data; }
    public int getPart() { return part; }
    public boolean isLast() { return last; }
}
