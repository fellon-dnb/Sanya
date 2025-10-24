package com.sanya.events.core;

/**
 * Расширенный EventBus с подпиской, отпиской и поддержкой иерархии событий.
 */
public interface EventBus {

    /**
     * Подписывает обработчик на указанный тип события.
     */
    <E> void subscribe(Class<E> eventType, EventHandler<? super E> handler);

    /**
     * Отписывает обработчик от указанного типа события.
     */
    <E> void unsubscribe(Class<E> eventType, EventHandler<? super E> handler);

    /** Публикует событие по его реальному типу. */
    void publish(Object event);

    /** Публикует событие с явным указанием класса (для совместимости). */
    void publish(Class<?> eventClass, Object event);
}