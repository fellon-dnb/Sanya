package com.sanya.client.core.api;

public interface Transport {
    void connect();
    void send(Object message);
    void close();
}
