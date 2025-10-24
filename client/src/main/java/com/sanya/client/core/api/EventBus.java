package com.sanya.client.core.api;

import java.util.function.Consumer;

public interface EventBus {
    <T> void subscribe(Class<T> type, Consumer<? super T> handler);
    void publish(Object event);
}
