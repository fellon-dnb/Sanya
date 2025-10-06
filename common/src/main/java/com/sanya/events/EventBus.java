package com.sanya.events;

import java.util.function.Consumer;

public interface EventBus {
    /**
     * Подписывает обработчик на событие определённого типа.
     *
     * @param eventType класс события
     * @param handler   функция-обработчик
     * @param <E>       тип события
     */
    <E> void subscribe(Class<E> eventType, Consumer<E> handler);

    /**
     * Публикует событие для всех подписчиков.
     *
     * @param event объект события
     */
    void publish(Object event);
}
