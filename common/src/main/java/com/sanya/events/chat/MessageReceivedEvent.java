package com.sanya.events.chat;
import com.sanya.Message;

// Событие: пришло сообщение от сервера
public record MessageReceivedEvent(Message message) implements MessageEvent {
    @Override
    public String toString() {
        return "[Event] MessageRecived: " + message;
    }
}
