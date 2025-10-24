package com.sanya.client.security;

import com.sanya.client.core.api.CryptoEngine;
import com.sanya.crypto.Crypto;

import javax.crypto.SecretKey;

public final class Encryptor implements CryptoEngine {
    private final KeyDirectory dir;

    public Encryptor(KeyDirectory dir) {
        this.dir = dir;
    }

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

    @Override
    public byte[] encrypt(byte[] data, String recipient) {
        Enc e = encryptFor(recipient, data);
        // возвращаем nonce + ct, вызывающий должен знать формат
        byte[] out = new byte[e.nonce.length + e.ct.length];
        System.arraycopy(e.nonce, 0, out, 0, e.nonce.length);
        System.arraycopy(e.ct, 0, out, e.nonce.length, e.ct.length);
        return out;
    }

    @Override
    public byte[] decrypt(byte[] data, String sender) {
        if (data.length < 12) throw new IllegalArgumentException("Invalid encrypted payload");
        byte[] nonce = new byte[12];
        byte[] ct = new byte[data.length - 12];
        System.arraycopy(data, 0, nonce, 0, 12);
        System.arraycopy(data, 12, ct, 0, ct.length);
        return decryptFrom(sender, nonce, ct);
    }
}
