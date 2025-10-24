package com.sanya.client;

import com.sanya.client.core.CommandHandler;
import com.sanya.client.core.AppCore;
import com.sanya.client.core.EventSubscriptionsManager;
import com.sanya.client.core.ServiceRegistry;
import com.sanya.client.core.DependencyContainer;
import com.sanya.client.service.ChatService;
import com.sanya.client.service.audio.VoiceService;
import com.sanya.client.settings.NetworkSettings;
import com.sanya.client.settings.UiSettings;
import com.sanya.client.settings.UserSettings;
import com.sanya.client.facade.UIFacade;
import com.sanya.crypto.KeyUtils;
import com.sanya.crypto.SignedPreKeyBundle;
import com.sanya.events.core.DefaultEventBus;
import com.sanya.events.core.SimpleDefaultEventBus;

import java.security.KeyPair;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ApplicationContext {

    private final DependencyContainer di = new DependencyContainer();

    private final NetworkSettings networkSettings;
    private final UserSettings userSettings = new UserSettings();
    private final UiSettings uiSettings = new UiSettings();
    private final DefaultEventBus defaultEventBus = new SimpleDefaultEventBus();
    private final CommandHandler commandHandler = new CommandHandler(this);
    private final AppCore core = new AppCore(this);
    private UIFacade uiFacade;
    private EventSubscriptionsManager eventSubscriptionsManager;
    private KeyPair x25519KeyPair;
    private KeyPair ed25519KeyPair;
    private final Map<String, SignedPreKeyBundle> knownBundles = new ConcurrentHashMap<>();

    public ApplicationContext(NetworkSettings networkSettings) {
        this.networkSettings = networkSettings;
        try {
            this.x25519KeyPair = KeyUtils.generateX25519();
            this.ed25519KeyPair = KeyUtils.generateEd25519();
        } catch (Exception e) {
            throw new RuntimeException("Failed to init crypto keys", e);
        }
        di.registerSingleton(DefaultEventBus.class, () -> defaultEventBus);
        di.registerSingleton(ApplicationContext.class, () -> this);
        di.registerSingleton(ChatService.class, () -> core.services().chat());
        di.registerSingleton(VoiceService.class, () -> core.services().voice());
        di.registerSingleton(EventSubscriptionsManager.class, () -> eventSubscriptionsManager);
    }

    public <T> T get(Class<T> type) {
        return di.get(type);
    }

    public DependencyContainer di() { return di; }
    public DefaultEventBus getEventBus() { return defaultEventBus; }
    public NetworkSettings getNetworkSettings() { return networkSettings; }
    public UiSettings getUiSettings() { return uiSettings; }
    public UserSettings getUserSettings() { return userSettings; }
    public CommandHandler getCommandHandler() { return commandHandler; }

    public AppCore core() { return core; }
    public ServiceRegistry services() { return core.services(); }

    public UIFacade getUIFacade() {
        return uiFacade != null ? uiFacade : di.get(UIFacade.class);
    }

    public void setUIFacade(UIFacade uiFacade) {
        this.uiFacade = uiFacade;
        di.registerSingleton(UIFacade.class, () -> uiFacade);
    }
    public EventSubscriptionsManager getEventSubscriptionsManager() {
        return eventSubscriptionsManager;
    }

    public void setEventSubscriptionsManager(EventSubscriptionsManager eventSubscriptionsManager) {
        this.eventSubscriptionsManager = eventSubscriptionsManager;
    }
    public KeyPair getX25519KeyPair() { return x25519KeyPair; }
    public KeyPair getEd25519KeyPair() { return ed25519KeyPair; }
    public Map<String, SignedPreKeyBundle> getKnownBundles() { return knownBundles; }
}
