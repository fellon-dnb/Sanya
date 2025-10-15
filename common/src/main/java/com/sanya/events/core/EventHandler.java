package com.sanya.events.core;

/**
 * Типобезопасный обработчик событий.
 */
@FunctionalInterface
public interface EventHandler<E> {
    void handle(E event);
}
