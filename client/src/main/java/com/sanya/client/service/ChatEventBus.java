package com.sanya.client.service;

import com.sanya.client.core.api.EventBus;
import com.sanya.events.core.DefaultEventBus;

import java.util.function.Consumer;

public class ChatEventBus implements EventBus {

    private final DefaultEventBus delegate;

    public ChatEventBus(DefaultEventBus delegate) {
        this.delegate = delegate;
    }

    @Override
    public <T> void subscribe(Class<T> type, Consumer<? super T> handler) {
        delegate.subscribe(type, handler::accept);
    }

    @Override
    public void publish(Object event) {
        delegate.publish(event);
    }
}
