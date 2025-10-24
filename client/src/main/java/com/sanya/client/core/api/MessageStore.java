package com.sanya.client.core.api;

import java.util.List;

public interface MessageStore {
    void save(String recipient, Object message);
    List<Object> retrieve(String recipient);
}
