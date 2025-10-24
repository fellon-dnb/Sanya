package com.sanya.client.core.api;

public interface KeyRepository {
    void store(String userId, byte[] publicKey);
    byte[] get(String userId);
}
