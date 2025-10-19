package com.sanya.crypto;

import java.io.Serializable;
/**
 Контейнер публичных ключей и подписи;
 */
public final class SignedPreKeyBundle implements Serializable {
    private final String username;
    private final byte[] x25519Public;  // 32 байта (наш формат)
    private final byte[] ed25519Public; // JCA-encoded (X.509 SubjectPublicKeyInfo)
    private final byte[] signature;     // Ed25519 подпись по (username || x25519Public)
    private final long timestamp;

    public SignedPreKeyBundle(String username, byte[] x25519Public, byte[] ed25519Public, byte[] signature, long timestamp) {
        this.username = username;
        this.x25519Public = x25519Public;
        this.ed25519Public = ed25519Public;
        this.signature = signature;
        this.timestamp = timestamp;
    }
    public String getUsername() { return username; }
    public byte[] getX25519Public() { return x25519Public; }
    public byte[] getEd25519Public() { return ed25519Public; }
    public byte[] getSignature() { return signature; }
    public long getTimestamp() { return timestamp; }
}
