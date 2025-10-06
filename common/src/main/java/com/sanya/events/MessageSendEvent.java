package com.sanya.events;
// Событие: пользователь хочет отправить сообщение
public record MessageSendEvent(String text) {
}
