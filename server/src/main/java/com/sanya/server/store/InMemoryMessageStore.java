package com.sanya.server.store;

import java.util.*;
import java.util.concurrent.*;

public class InMemoryMessageStore implements MessageStore {
    private final Map<String, Queue<Object>> inbox = new ConcurrentHashMap<>();

    @Override
    public void save(String recipient, Object message) {
        inbox.computeIfAbsent(recipient, k -> new ConcurrentLinkedQueue<>()).add(message);
    }

    @Override
    public List<Object> retrieve(String recipient) {
        Queue<Object> queue = inbox.remove(recipient);
        return queue == null ? List.of() : new ArrayList<>(queue);
    }
}
