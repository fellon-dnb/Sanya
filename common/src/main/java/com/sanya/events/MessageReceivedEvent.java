package com.sanya.events;
import com.sanya.Message;

// Событие: пришло сообщение от сервера
public record MessageReceivedEvent(Message message) {
}
