package com.sanya.client.security;

import com.sanya.crypto.Crypto;

import javax.crypto.SecretKey;

public final class Encryptor {
    private final KeyDirectory dir;

    public Encryptor(KeyDirectory dir) { this.dir = dir; }

    public record Enc(byte[] nonce, byte[] ct) {}

    public Enc encryptFor(String toUser, byte[] plain) {
        SecretKey k = dir.getOrDeriveSession(toUser);
        if (k == null) throw new IllegalStateException("No pubkey for " + toUser);
        byte[] n = Crypto.randomNonce12();
        return new Enc(n, Crypto.encryptGCM(k, n, plain));
    }

    public byte[] decryptFrom(String fromUser, byte[] nonce, byte[] ct) {
        SecretKey k = dir.getOrDeriveSession(fromUser);
        if (k == null) throw new IllegalStateException("No pubkey for " + fromUser);
        return Crypto.decryptGCM(k, nonce, ct);
    }
}
