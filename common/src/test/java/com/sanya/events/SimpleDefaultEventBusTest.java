package com.sanya.events;

import com.sanya.Message;
import com.sanya.events.chat.MessageEvent;
import com.sanya.events.chat.MessageReceivedEvent;
import com.sanya.events.chat.MessageSendEvent;
import com.sanya.events.core.EventBus;
import com.sanya.events.core.SimpleEventBus;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SimpleEventBusTest {

    @Test
    void testHierarchyEventDelivery() {
        EventBus bus = new SimpleEventBus();

        AtomicInteger counter = new AtomicInteger(0);

        // подписка на общий тип MessageEvent
        bus.subscribe(MessageEvent.class, e -> counter.incrementAndGet());

        // подписка на конкретный тип
        bus.subscribe(MessageReceivedEvent.class, e -> counter.addAndGet(10));

        // публикуем разные события
        bus.publish(new MessageSendEvent("hello"));
        bus.publish(new MessageReceivedEvent(new Message("Bob", "hi")));

        // 1-е событие — только общий обработчик (1)
        // 2-е событие — оба (1 + 10)
        assertEquals(12, counter.get());
    }
}
