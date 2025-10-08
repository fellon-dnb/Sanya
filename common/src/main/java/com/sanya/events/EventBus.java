package com.sanya.events;

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

    /**
     * Публикует событие.
     */
    void publish(Object event);
}
