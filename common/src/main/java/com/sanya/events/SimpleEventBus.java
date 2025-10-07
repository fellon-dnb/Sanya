package com.sanya.events;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Потокобезопасная реализация EventBus с поддержкой иерархии событий и логированием.
 */
public class SimpleEventBus implements EventBus {

    private static final Logger log = Logger.getLogger(SimpleEventBus.class.getName());

    private final Map<Class<?>, List<EventHandler<?>>> subscribers = new ConcurrentHashMap<>();

    @Override
    public <E> void subscribe(Class<E> eventType, EventHandler<? super E> handler) {
        subscribers.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(handler);
        log.fine(() -> "Subscribed handler " + handler + " to " + eventType.getSimpleName());
    }

    @Override
    public <E> void unsubscribe(Class<E> eventType, EventHandler<? super E> handler) {
        List<EventHandler<?>> handlers = subscribers.get(eventType);
        if (handlers != null) {
            handlers.remove(handler);
            if (handlers.isEmpty()) {
                subscribers.remove(eventType);
            }
            log.fine(() -> "Unsubscribed handler " + handler + " from " + eventType.getSimpleName());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void publish(Object event) {
        if (event == null) return;

        Class<?> eventClass = event.getClass();
        Set<Class<?>> allEventTypes = collectHierarchy(eventClass);

        boolean delivered = false;

        for (Class<?> type : allEventTypes) {
            List<EventHandler<?>> handlers = subscribers.get(type);
            if (handlers != null) {
                for (EventHandler<?> handler : handlers) {
                    try {
                        ((EventHandler<Object>) handler).handle(event);
                        delivered = true;
                    } catch (Exception e) {
                        log.log(Level.SEVERE, "Error in event handler for " + type.getSimpleName(), e);
                    }
                }
            }
        }

        if (!delivered) {
            log.fine(() -> "No subscribers for event " + eventClass.getSimpleName());
        }
    }

    private Set<Class<?>> collectHierarchy(Class<?> clazz) {
        Set<Class<?>> result = new LinkedHashSet<>();
        while (clazz != null && clazz != Object.class) {
            result.add(clazz);
            result.addAll(Arrays.asList(clazz.getInterfaces()));
            clazz = clazz.getSuperclass();
        }
        return result;
    }
}
