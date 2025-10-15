package com.sanya.client.core;

import com.sanya.client.ApplicationContext;
import com.sanya.events.core.EventBus;

/**
 * AppCore — ядро DI и базовой инфраструктуры (EventBus, ServiceRegistry)
 */
public final class AppCore {

    private final DependencyContainer di = new DependencyContainer();
    private final EventBus eventBus;
    private final ServiceRegistry services;

    public AppCore(ApplicationContext ctx) {
        this.eventBus = ctx.getEventBus();
        services = new ServiceRegistry(ctx, eventBus);
        di.registerSingleton(ServiceRegistry.class, () -> services);
    }

    public EventBus eventBus() { return eventBus; }
    public ServiceRegistry services() { return services; }
}
