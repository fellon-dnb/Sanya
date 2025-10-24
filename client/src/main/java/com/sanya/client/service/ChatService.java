package com.sanya.client.service;

import com.sanya.client.core.api.EventBus;
import com.sanya.events.chat.MessageSendEvent;
import com.sanya.events.core.DefaultEventBus;
import com.sanya.events.system.SystemMessageEvent;
import com.sanya.events.ui.ClearChatEvent;

import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.sanya.messages.VoiceMessage;
/**
 * ChatService управляет взаимодействием чата через EventBus.
 * Работает с любым транспортом, переданным через attachOutputSupplier().
 */
public class ChatService {

    private static final Logger log = Logger.getLogger(ChatService.class.getName());

    private final EventBus bus;
    private Supplier<Boolean> isConnectedSupplier;
    private Consumer<Object> objectSender;

    public ChatService(EventBus bus) {
        this.bus = bus;
    }

    public void attachOutputSupplier(Supplier<Boolean> isConnectedSupplier, Consumer<Object> objectSender) {
        this.isConnectedSupplier = isConnectedSupplier;
        this.objectSender = objectSender;
        log.info("ChatService transport attached");
    }

    public void sendMessage(String text) {
        bus.publish(new MessageSendEvent(text));
        log.fine("Published MessageSendEvent: " + text);
    }

    public void clearChat() {
        bus.publish(new ClearChatEvent());
        log.info("Chat cleared");
    }

    public boolean isConnected() {
        boolean connected = isConnectedSupplier != null && isConnectedSupplier.get();
        log.fine("Connection check: " + connected);
        return connected;
    }

    public void sendObject(Object obj) {
        if (objectSender == null) {
            log.warning("Attempt to send object with no active transport");
            bus.publish(new SystemMessageEvent("[ERROR] Transport not attached"));
            return;
        }
        try {
            objectSender.accept(obj);
            log.fine("Object sent: " + obj.getClass().getSimpleName());

            // отразить отправленное приватное сообщение в UI
            if (obj instanceof com.sanya.crypto.msg.EncryptedDirectMessage dm) {
                var m = new com.sanya.Message(dm.from(), "[private] " + "[you]"); // или текст, если его знаешь
                bus.publish(new com.sanya.events.chat.MessageReceivedEvent(m));
            }

        } catch (Exception e) {
            log.log(Level.SEVERE, "Send failed", e);
            bus.publish(new SystemMessageEvent("[ERROR] Send failed: " + e.getMessage()));
        }
    }


    public EventBus getBus() {
        return bus;
    }
    public void sendVoiceMessage(byte[] data, String recipient) {
        sendObject(new VoiceMessage(recipient , data));
    }

}