package com.sanya.server.store;

import java.util.List;

public interface MessageStore {
    void save(String recipient, Object message);
    List<Object> retrieve(String recipient);
}
