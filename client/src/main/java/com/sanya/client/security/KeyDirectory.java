package com.sanya.client.security;

import com.sanya.crypto.Crypto;

import javax.crypto.SecretKey;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class KeyDirectory {
    private final KeyPair my; // X25519
    private final Map<String, PublicKey> pub = new ConcurrentHashMap<>();
    private final Map<String, SecretKey> session = new ConcurrentHashMap<>();

    public KeyDirectory() { this.my = Crypto.genX25519(); }

    public KeyPair myKeyPair() { return my; }

    public void putPub(String user, PublicKey k) { pub.put(user, k); session.remove(user); }
    public PublicKey getPub(String user) { return pub.get(user); }

    public SecretKey getOrDeriveSession(String user) {
        return session.computeIfAbsent(user, u -> {
            PublicKey p = pub.get(u); if (p == null) return null;
            return Crypto.deriveX25519(my.getPrivate(), p);
        });
    }

    public Map<String, PublicKey> allPubs() { return Map.copyOf(pub); }
}
