package com.sanya.client.service;

import com.sanya.client.core.api.EventBus;
import com.sanya.events.chat.MessageSendEvent;
import com.sanya.events.system.SystemMessageEvent;
import com.sanya.events.ui.ClearChatEvent;
import com.sanya.messages.VoiceMessage;

import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ChatService — сервис для управления обменом сообщениями через {@link EventBus}.
 * Отвечает за взаимодействие с транспортным уровнем, публикацию событий и передачу данных.
 *
 * Назначение:
 *  - Обеспечить единый интерфейс между UI, EventBus и сетевым транспортом.
 *  - Публиковать события сообщений, очистки чата и системных уведомлений.
 *  - Отправлять объекты и голосовые сообщения через установленный транспорт.
 *
 * Использование:
 *  chatService.attachOutputSupplier(socket::isConnected, socket::send);
 *  chatService.sendMessage("Hello");
 *  chatService.sendVoiceMessage(audioData, "user");
 */
public final class ChatService {

    private static final Logger log = Logger.getLogger(ChatService.class.getName());

    private final EventBus bus;
    private Supplier<Boolean> isConnectedSupplier;
    private Consumer<Object> objectSender;

    public ChatService(EventBus bus) {
        this.bus = bus;
    }

    /**
     * Подключает транспорт для отправки объектов.
     *
     * @param isConnectedSupplier функция проверки соединения
     * @param objectSender        функция отправки объекта
     */
    public void attachOutputSupplier(Supplier<Boolean> isConnectedSupplier, Consumer<Object> objectSender) {
        this.isConnectedSupplier = isConnectedSupplier;
        this.objectSender = objectSender;
        log.info("ChatService transport attached");
    }

    /** Публикует событие отправки текстового сообщения. */
    public void sendMessage(String text) {
        bus.publish(new MessageSendEvent(text));
        log.fine("Published MessageSendEvent: " + text);
    }

    /** Публикует событие очистки чата. */
    public void clearChat() {
        bus.publish(new ClearChatEvent());
        log.info("Chat cleared");
    }

    /** Проверяет состояние соединения с транспортом. */
    public boolean isConnected() {
        boolean connected = isConnectedSupplier != null && isConnectedSupplier.get();
        log.fine("Connection check: " + connected);
        return connected;
    }

    /**
     * Отправляет объект через подключённый транспорт.
     * При ошибке публикует {@link SystemMessageEvent}.
     */
    public void sendObject(Object obj) {
        if (objectSender == null) {
            log.warning("Attempt to send object with no active transport");
            bus.publish(new SystemMessageEvent("[ERROR] Transport not attached"));
            return;
        }

        try {
            objectSender.accept(obj);
            log.fine("Object sent: " + obj.getClass().getSimpleName());

            // Отражение приватного сообщения в UI при шифрованной отправке
            if (obj instanceof com.sanya.crypto.msg.EncryptedDirectMessage dm) {
                var m = new com.sanya.Message(dm.from(), "[private] [you]");
                bus.publish(new com.sanya.events.chat.MessageReceivedEvent(m));
            }

        } catch (Exception e) {
            log.log(Level.SEVERE, "Send failed", e);
            bus.publish(new SystemMessageEvent("[ERROR] Send failed: " + e.getMessage()));
        }
    }

    /** Возвращает EventBus, связанный с чатом. */
    public EventBus getBus() {
        return bus;
    }

    /** Отправляет голосовое сообщение получателю. */
    public void sendVoiceMessage(byte[] data, String recipient) {
        sendObject(new VoiceMessage(recipient, data));
    }
}
