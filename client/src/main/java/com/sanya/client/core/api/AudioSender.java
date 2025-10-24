package com.sanya.client.core.api;

public interface AudioSender {
    void send(byte[] audioChunk, String recipient);
}
