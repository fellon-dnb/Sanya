package com.sanya.client;

import com.sanya.client.core.*;
import com.sanya.client.facade.UIFacade;
import com.sanya.client.service.ChatService;
import com.sanya.client.service.audio.VoiceService;
import com.sanya.client.settings.NetworkSettings;
import com.sanya.client.settings.UiSettings;
import com.sanya.client.settings.UserSettings;
import com.sanya.crypto.KeyUtils;
import com.sanya.crypto.SignedPreKeyBundle;
import com.sanya.events.core.DefaultEventBus;
import com.sanya.events.core.SimpleDefaultEventBus;

import java.security.KeyPair;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ApplicationContext — центральная точка доступа к инфраструктуре клиента.
 * Содержит все основные сервисы, настройки, ключи шифрования и DI-контейнер.
 *
 * Назначение:
 * - Управлять жизненным циклом и зависимостями всех компонент клиента.
 * - Предоставлять доступ к сервисам, настройкам, шине событий и криптографическим ключам.
 * - Служить единым хранилищем контекста для UI, сетевого уровня и сервисов.
 *
 * Основные компоненты:
 *  - DependencyContainer — DI-контейнер для регистрации и получения зависимостей.
 *  - EventBus — маршрутизация событий между модулями.
 *  - ServiceRegistry — набор сервисов (чат, голос, темы, файлы).
 *  - CommandHandler — обработчик REPL-команд.
 *  - Ключи X25519 и Ed25519 для шифрования и подписей.
 *
 * Потокобезопасность:
 * Использует ConcurrentHashMap для хранения известных криптографических пакетов.
 */
public final class ApplicationContext {

    /** Контейнер зависимостей. */
    private final DependencyContainer di = new DependencyContainer();

    /** Сетевые настройки (адрес и порт сервера). */
    private final NetworkSettings networkSettings;

    /** Настройки пользователя (имя и профиль). */
    private final UserSettings userSettings = new UserSettings();

    /** Настройки интерфейса (тема, звук и т.д.). */
    private final UiSettings uiSettings = new UiSettings();

    /** Основная шина событий клиента. */
    private final DefaultEventBus defaultEventBus = new SimpleDefaultEventBus();

    /** Обработчик команд REPL. */
    private final CommandHandler commandHandler = new CommandHandler(this);

    /** Основное ядро приложения, включающее ServiceRegistry. */
    private final AppCore core = new AppCore(this);

    /** Глобальный интерфейсный фасад. */
    private UIFacade uiFacade;

    /** Менеджер подписок на события. */
    private EventSubscriptionsManager eventSubscriptionsManager;

    /** Криптографические пары ключей (X25519 — обмен, Ed25519 — подпись). */
    private KeyPair x25519KeyPair;
    private KeyPair ed25519KeyPair;

    /** Известные публичные ключи других пользователей (pre-key bundles). */
    private final Map<String, SignedPreKeyBundle> knownBundles = new ConcurrentHashMap<>();

    /**
     * Конструктор инициализирует контекст и основные зависимости.
     *
     * @param networkSettings сетевые настройки (хост, порт)
     */
    public ApplicationContext(NetworkSettings networkSettings) {
        this.networkSettings = networkSettings;

        try {
            this.x25519KeyPair = KeyUtils.generateX25519();
            this.ed25519KeyPair = KeyUtils.generateEd25519();
        } catch (Exception e) {
            throw new RuntimeException("Failed to init crypto keys", e);
        }

        // Регистрация основных компонентов в DI-контейнере
        di.registerSingleton(DefaultEventBus.class, () -> defaultEventBus);
        di.registerSingleton(ApplicationContext.class, () -> this);
        di.registerSingleton(ChatService.class, () -> core.services().chat());
        di.registerSingleton(VoiceService.class, () -> core.services().voice());
        di.registerSingleton(EventSubscriptionsManager.class, () -> eventSubscriptionsManager);
    }

    /** Возвращает экземпляр по типу из DI-контейнера. */
    public <T> T get(Class<T> type) {
        return di.get(type);
    }

    public DependencyContainer di() { return di; }
    public DefaultEventBus getEventBus() { return defaultEventBus; }
    public NetworkSettings getNetworkSettings() { return networkSettings; }
    public UiSettings getUiSettings() { return uiSettings; }
    public UserSettings getUserSettings() { return userSettings; }
    public CommandHandler getCommandHandler() { return commandHandler; }

    /** Возвращает ядро приложения. */
    public AppCore core() { return core; }

    /** Возвращает реестр сервисов. */
    public ServiceRegistry services() { return core.services(); }

    /** Возвращает UI-фасад (если не установлен — пытается получить из DI). */
    public UIFacade getUIFacade() {
        return uiFacade != null ? uiFacade : di.get(UIFacade.class);
    }

    /** Устанавливает фасад интерфейса и регистрирует его в DI. */
    public void setUIFacade(UIFacade uiFacade) {
        this.uiFacade = uiFacade;
        di.registerSingleton(UIFacade.class, () -> uiFacade);
    }

    /** Возвращает менеджер подписок. */
    public EventSubscriptionsManager getEventSubscriptionsManager() {
        return eventSubscriptionsManager;
    }

    /** Устанавливает менеджер подписок на события. */
    public void setEventSubscriptionsManager(EventSubscriptionsManager eventSubscriptionsManager) {
        this.eventSubscriptionsManager = eventSubscriptionsManager;
    }

    /** Возвращает пару ключей X25519 для обмена. */
    public KeyPair getX25519KeyPair() { return x25519KeyPair; }

    /** Возвращает пару ключей Ed25519 для подписей. */
    public KeyPair getEd25519KeyPair() { return ed25519KeyPair; }

    /** Возвращает известные криптографические пакеты других пользователей. */
    public Map<String, SignedPreKeyBundle> getKnownBundles() { return knownBundles; }
}
