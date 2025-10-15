package com.sanya.client.service;

import com.sanya.events.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * ChatService управляет взаимодействием чата через EventBus.
 * Работает с любым транспортом, переданным через attachOutputSupplier().
 */
public class ChatService {

    private final EventBus bus;

    // supplier проверяет активность соединения
    private Supplier<Boolean> isConnectedSupplier;
    // sender отвечает за отправку объектов
    private Consumer<Object> objectSender;

    public ChatService(EventBus bus) {
        this.bus = bus;
    }

    /**
     * Привязка транспорта: логика отправки и проверки соединения.
     */
    public void attachOutputSupplier(Supplier<Boolean> isConnectedSupplier,
                                     Consumer<Object> objectSender) {
        this.isConnectedSupplier = isConnectedSupplier;
        this.objectSender = objectSender;
    }

    /** Отправка текстового сообщения. */
    public void sendMessage(String text) {
        bus.publish(new MessageSendEvent(text));
    }

    /** Очистка чата. */
    public void clearChat() {
        bus.publish(new ClearChatEvent());
    }

    /** Проверка активного соединения. */
    public boolean isConnected() {
        return isConnectedSupplier != null && isConnectedSupplier.get();
    }

    /** Отправка объекта (файл, голос, и т.п.) напрямую в транспорт. */
    public void sendObject(Object obj) {
        if (objectSender == null) {
            bus.publish(new SystemMessageEvent("[ERROR] Transport not attached"));
            return;
        }
        try {
            objectSender.accept(obj);
        } catch (Exception e) {
            bus.publish(new SystemMessageEvent("[ERROR] Send failed: " + e.getMessage()));
        }
    }

    /** Получение EventBus. */
    public EventBus getBus() {
        return bus;
    }
}
