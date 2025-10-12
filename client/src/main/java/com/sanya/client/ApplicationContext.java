package com.sanya.client;

import com.sanya.client.commands.CommandHandler;
import com.sanya.client.settings.NetworkSettings;
import com.sanya.client.settings.UiSettings;
import com.sanya.client.settings.UserSettings;
import com.sanya.events.EventBus;
import com.sanya.events.SimpleEventBus;

public final class ApplicationContext {

    private final NetworkSettings networkSettings;
    private final UserSettings userSettings = new UserSettings();
    private final UiSettings uiSettings = new UiSettings();
    private final EventBus eventBus = new SimpleEventBus();
    private final CommandHandler commandHandler = new CommandHandler(this);

    public ApplicationContext(NetworkSettings networkSettings) {
        this.networkSettings = networkSettings;
    }

    public EventBus getEventBus() {
        return eventBus;
    }

    public NetworkSettings getNetworkSettings() {
        return networkSettings;
    }

    public UiSettings getUiSettings() {
        return uiSettings;
    }

    public UserSettings getUserSettings() {
        return userSettings;
    }

    public CommandHandler getCommandHandler() {
        return commandHandler;
    }
}
