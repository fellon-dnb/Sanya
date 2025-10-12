package com.sanya.client;

import com.sanya.client.commands.CommandHandler;
import com.sanya.client.service.ChatService;
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
    private final Services services = new Services(this);

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

    public Services services() {
        return services;
    }

    public static class Services {

        private final ChatService chatService;

        public Services(ApplicationContext ctx) {
            chatService = new ChatService(ctx.getEventBus());
        }

        public ChatService chat() {
            return chatService;
        }
    }
}
