package com.sanya.client.core;

import com.sanya.client.ApplicationContext;
import com.sanya.client.service.ChatService;
import com.sanya.client.service.audio.VoiceService;
import com.sanya.client.ui.theme.ThemeManager;
import com.sanya.events.EventBus;

/**
 * Реестр всех сервисов клиента (chat, voice и т.д.)
 */
public final class ServiceRegistry {

    private final ChatService chatService;
    private final VoiceService voiceService;
  private final ThemeManager themeManager;
    // принимает и ApplicationContext, и EventBus
    public ServiceRegistry(ApplicationContext ctx, EventBus bus) {
        this.chatService = new ChatService(bus);
        this.voiceService = new VoiceService(ctx);
        this.themeManager = new ThemeManager(ctx,bus);

    }

    public ChatService chat() {return chatService;}

    public VoiceService voice() {return voiceService;}
    public  ThemeManager theme() {return themeManager;}
}
