package com.sanya.events;

/**
 * Типобезопасный обработчик событий.
 */
@FunctionalInterface
public interface EventHandler<E> {
    void handle(E event);
}
