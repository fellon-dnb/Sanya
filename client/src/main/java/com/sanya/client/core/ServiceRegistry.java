package com.sanya.client.core;

import com.sanya.client.ApplicationContext;
import com.sanya.client.core.api.AudioSender;
import com.sanya.client.core.api.EventBus;
import com.sanya.client.core.api.FileTransferService;
import com.sanya.client.service.ChatService;
import com.sanya.client.service.audio.VoiceSender;
import com.sanya.client.service.audio.VoiceService;
import com.sanya.client.service.files.FileSender;
import com.sanya.client.ui.theme.ThemeManager;
import com.sanya.events.core.DefaultEventBus;
import com.sanya.client.core.api.EventBus;
/**
 * Реестр всех сервисов клиента
 */
public final class ServiceRegistry {

    private final ChatService chatService;
    private final VoiceService voiceService;
    private final ThemeManager themeManager;
    private final FileTransferService fileSender;
    private final AudioSender voiceSender;
    // принимает и ApplicationContext, и EventBus
    public ServiceRegistry(ApplicationContext ctx, EventBus bus) {
        this.chatService = new ChatService(bus);
        this.voiceService = new VoiceService(ctx);
        this.themeManager = new ThemeManager(ctx, bus);
        this.fileSender = new FileSender(bus);
        this.voiceSender = new VoiceSender(this.chatService);
    }

    public ChatService chat() {
        return chatService;
    }

    public VoiceService voice() {
        return voiceService;
    }

    public ThemeManager theme() {
        return themeManager;
    }

    public FileTransferService fileSender() {
        return fileSender;
    }
    public AudioSender voiceSender() {
        return voiceSender;
    }
}
