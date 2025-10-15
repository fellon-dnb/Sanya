package com.sanya.client;

import com.sanya.client.commands.CommandHandler;
import com.sanya.client.core.AppCore;
import com.sanya.client.core.ServiceRegistry;
import com.sanya.client.di.DependencyContainer;
import com.sanya.client.service.ChatService;
import com.sanya.client.service.audio.VoiceService;
import com.sanya.client.settings.NetworkSettings;
import com.sanya.client.settings.UiSettings;
import com.sanya.client.settings.UserSettings;
import com.sanya.client.ui.UIFacade;
import com.sanya.events.EventBus;
import com.sanya.events.SimpleEventBus;

public final class ApplicationContext {

    private final DependencyContainer di = new DependencyContainer();

    private final NetworkSettings networkSettings;
    private final UserSettings userSettings = new UserSettings();
    private final UiSettings uiSettings = new UiSettings();
    private final EventBus eventBus = new SimpleEventBus();
    private final CommandHandler commandHandler = new CommandHandler(this);
    private final AppCore core = new AppCore(this);
    private UIFacade uiFacade;

    public ApplicationContext(NetworkSettings networkSettings) {
        this.networkSettings = networkSettings;

        di.registerSingleton(EventBus.class, () -> eventBus);
        di.registerSingleton(ApplicationContext.class, () -> this);
        di.registerSingleton(ChatService.class, () -> core.services().chat());
        di.registerSingleton(VoiceService.class, () -> core.services().voice());
    }

    public <T> T get(Class<T> type) {
        return di.get(type);
    }

    public DependencyContainer di() { return di; }
    public EventBus getEventBus() { return eventBus; }
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
}
