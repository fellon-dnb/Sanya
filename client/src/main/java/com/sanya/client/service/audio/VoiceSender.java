package com.sanya.client.service.audio;

import com.sanya.client.core.api.AudioSender;
import com.sanya.client.service.ChatService;

public class VoiceSender implements AudioSender {

    private final ChatService chat;

    public VoiceSender(ChatService chat) {
        this.chat = chat;
    }

    @Override
    public void send(byte[] audioChunk, String recipient) {
        chat.sendVoiceMessage(audioChunk, recipient);
    }
}
