package com.sanya.client.core;

import com.sanya.client.ApplicationContext;
import com.sanya.client.core.api.EventBus;
import com.sanya.client.service.ChatEventBus;
import com.sanya.events.core.DefaultEventBus;

/**
 * AppCore — ядро клиентского приложения.
 * Отвечает за инициализацию DI-контейнера, шины событий и реестра сервисов.
 *
 * Назначение:
 *  - Централизует доступ к общим сервисам и событиям.
 *  - Оборачивает базовый EventBus (DefaultEventBus) адаптером ChatEventBus
 *    для унифицированного API и расширения функциональности.
 *  - Инкапсулирует инфраструктурные зависимости от ApplicationContext.
 *
 * Использование:
 *  AppCore создаётся внутри ApplicationContext и предоставляет
 *  доступ к сервисам (ServiceRegistry) и шине событий (EventBus).
 */
public final class AppCore {

    /** Контейнер зависимостей для внутренних сервисов */
    private final DependencyContainer di = new DependencyContainer();

    /** Базовая реализация EventBus из общего модуля */
    private final DefaultEventBus rawBus;

    /** Обёртка EventBus с адаптацией под клиентскую логику */
    private final EventBus bus;

    /** Реестр всех сервисов приложения */
    private final ServiceRegistry services;

    /**
     * Конструктор ядра приложения.
     *
     * @param ctx контекст приложения, предоставляющий исходные зависимости
     */
    public AppCore(ApplicationContext ctx) {
        this.rawBus = ctx.getEventBus();
        this.bus = new ChatEventBus(rawBus);
        this.services = new ServiceRegistry(ctx, bus);
        di.registerSingleton(ServiceRegistry.class, () -> services);
    }

    /**
     * Возвращает клиентскую шину событий.
     */
    public EventBus eventBus() { return bus; }

    /**
     * Возвращает реестр сервисов.
     */
    public ServiceRegistry services() { return services; }
}
