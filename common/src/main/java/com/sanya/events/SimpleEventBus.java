package com.sanya.events;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class SimpleEventBus implements EventBus {

    private final Map<Class<?>, List<Consumer<?>>> subscribers = new ConcurrentHashMap<>();

    @Override
    public <E> void subscribe(Class<E> eventType, Consumer<E> handler) {
        subscribers
                .computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
                .add(handler);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void publish(Object event) {
        List<Consumer<?>> handlers = subscribers.get(event.getClass());
        if (handlers != null) {
            for (Consumer<?> handler : handlers) {
                ((Consumer<Object>) handler).accept(event);
            }
        }
    }
}
