package com.sanya.client.service;

import com.sanya.client.core.api.EventBus;
import com.sanya.events.core.DefaultEventBus;

import java.util.function.Consumer;

/**
 * ChatEventBus — адаптер между {@link DefaultEventBus} и клиентским интерфейсом {@link EventBus}.
 *
 * Назначение:
 *  - Обеспечить унифицированный интерфейс событий для клиентской логики.
 *  - Инкапсулировать конкретную реализацию {@link DefaultEventBus}.
 *  - Упрощать подписку и публикацию событий.
 *
 * Использование:
 *  ChatEventBus bus = new ChatEventBus(defaultBus);
 *  bus.subscribe(MessageEvent.class, this::onMessage);
 *  bus.publish(new UserConnectedEvent(...));
 */
public final class ChatEventBus implements EventBus {

    /** Делегат, выполняющий фактическую маршрутизацию событий. */
    private final DefaultEventBus delegate;

    /** Создаёт адаптер, оборачивающий {@link DefaultEventBus}. */
    public ChatEventBus(DefaultEventBus delegate) {
        this.delegate = delegate;
    }

    /** Подписывает обработчик на указанный тип события. */
    @Override
    public <T> void subscribe(Class<T> type, Consumer<? super T> handler) {
        delegate.subscribe(type, handler::accept);
    }

    /** Публикует событие для всех подписчиков. */
    @Override
    public void publish(Object event) {
        delegate.publish(event);
    }
}
