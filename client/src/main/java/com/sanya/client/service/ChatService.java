package com.sanya.client.service;

import com.sanya.client.ChatClientConnector;
import com.sanya.events.*;

/**
 * ChatService управляет взаимодействием чата через EventBus.
 * Также хранит ссылку на ChatClientConnector для передачи файлов и данных.
 */
public class ChatService {
    private final EventBus bus;
    private ChatClientConnector connector;

    public ChatService(EventBus bus) {
        this.bus = bus;
    }

    /** Привязать сетевой коннектор после подключения */
    public void attachConnector(ChatClientConnector connector) {
        this.connector = connector;
    }

    /** Отправка обычного текстового сообщения */
    public void sendMessage(String text) {
        bus.publish(new MessageSendEvent(text));
    }

    /** Очистка чата */
    public void clearChat() {
        bus.publish(new ClearChatEvent());
    }

    /** Получение EventBus */
    public EventBus getBus() {
        return bus;
    }

    /** Доступ к ObjectOutputStream для передачи файлов и голосовых данных */
    public java.io.ObjectOutputStream getOutputStream() {
        if (connector == null) {
            throw new IllegalStateException("ChatClientConnector not attached to ChatService");
        }
        return connector.getOutputStream();
    }

    public void onMessageReceived(EventHandler<MessageReceivedEvent> handler) {
        bus.subscribe(MessageReceivedEvent.class, handler);
    }

    public void onUserListUpdated(EventHandler<UserListUpdatedEvent> handler) {
        bus.subscribe(UserListUpdatedEvent.class, handler);
    }
}
