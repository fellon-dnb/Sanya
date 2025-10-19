package com.sanya.crypto.msg;

import java.io.Serializable;

// Заголовок маршрутизации открыт, payload шифруется end-to-end
public record EncryptedDirectMessage(
        String from, String to,
        byte[] nonce12, byte[] ciphertext,
        String mediaType, // "text/plain", "audio/pcm", "file/chunk"
        Integer chunkIndex, Boolean lastChunk
) implements Serializable {}
