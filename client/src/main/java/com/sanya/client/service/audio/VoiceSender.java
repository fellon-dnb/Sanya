package com.sanya.client.service.audio;

import com.sanya.client.core.api.AudioSender;
import com.sanya.client.service.ChatService;

/**
 * VoiceSender — реализация интерфейса {@link AudioSender}.
 * Отвечает за передачу аудиофрагментов через чат-сервис.
 *
 * Назначение:
 *  - Инкапсулировать логику отправки голосовых сообщений.
 *  - Делегировать фактическую отправку в {@link ChatService}.
 *
 * Использование:
 *  VoiceSender sender = new VoiceSender(chatService);
 *  sender.send(audioBytes, "username");
 */
public final class VoiceSender implements AudioSender {

    /** Сервис чата, выполняющий сетевую отправку данных. */
    private final ChatService chat;

    /** Создаёт новый экземпляр отправителя аудио. */
    public VoiceSender(ChatService chat) {
        this.chat = chat;
    }

    /**
     * Отправляет фрагмент аудиозаписи указанному получателю.
     *
     * @param audioChunk массив байт аудиоданных
     * @param recipient  имя или идентификатор получателя
     */
    @Override
    public void send(byte[] audioChunk, String recipient) {
        chat.sendVoiceMessage(audioChunk, recipient);
    }
}
