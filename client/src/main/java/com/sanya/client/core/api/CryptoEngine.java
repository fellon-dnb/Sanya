package com.sanya.client.core.api;

public interface CryptoEngine {
    byte[] encrypt(byte[] data, String recipient);
    byte[] decrypt(byte[] data, String sender);
}
