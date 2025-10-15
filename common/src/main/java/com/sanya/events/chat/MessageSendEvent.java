package com.sanya.events.chat;
// Событие: пользователь хочет отправить сообщение
public record MessageSendEvent(String text) implements MessageEvent {
    @Override
    public String toString() {
        return "[Event] MessageSend: " + text;
    }
}
