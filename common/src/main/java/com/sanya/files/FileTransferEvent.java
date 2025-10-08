package com.sanya.files;

public record FileTransferEvent(
        Type type,
        String filename,
        long transferredBytes,
        long totalBytes,
        boolean outgoing,
        String errorMessage
) {
    public enum Type {
        STARTED,
        PROGRESS,
        COMPLETED,
        FAILED
    }
}
