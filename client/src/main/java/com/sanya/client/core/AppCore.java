package com.sanya.client.core;

import com.sanya.client.ApplicationContext;
import com.sanya.client.core.api.EventBus;
import com.sanya.client.service.ChatEventBus;
import com.sanya.client.service.audio.VoiceSender;
import com.sanya.client.service.files.FileSender;
import com.sanya.client.ui.theme.ThemeManager;
import com.sanya.events.core.DefaultEventBus;

public final class AppCore {

    private final DependencyContainer di = new DependencyContainer();
    private final DefaultEventBus rawBus;
    private final EventBus bus;
    private final ServiceRegistry services;

    public AppCore(ApplicationContext ctx) {
        this.rawBus = ctx.getEventBus();
        this.bus = new ChatEventBus(rawBus);
        this.services = new ServiceRegistry(ctx, bus);
        di.registerSingleton(ServiceRegistry.class, () -> services);
    }

    public EventBus eventBus() { return bus; }

    public ServiceRegistry services() { return services; }
}
