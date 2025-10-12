package com.sanya.client;

import com.sanya.client.commands.CommandHandler;
import com.sanya.events.EventBus;
import com.sanya.events.SimpleEventBus;
import com.sanya.events.Theme;

public final class ApplicationContext {

    private final ConnectionInfo connectionInfo;
    private final UserInfo userInfo = new UserInfo();
    private final EventBus eventBus = new SimpleEventBus();
    private final CommandHandler commandHandler = new CommandHandler(this);
    private Theme theme;
    private boolean soundEnabled;

    public ApplicationContext(ConnectionInfo connectionInfo) {
        this.connectionInfo = connectionInfo;
    }

    public ConnectionInfo getConnectionInfo() {
        return connectionInfo;
    }

    public EventBus getEventBus() {
        return eventBus;
    }

    public CommandHandler getCommandHandler() {
        return commandHandler;
    }

    public Theme getTheme() {
        return theme;
    }

    public void setTheme(Theme theme) {
        this.theme = theme;
    }

    public boolean isSoundEnabled() {
        return soundEnabled;
    }

    public void setSoundEnabled(boolean soundEnabled) {
        this.soundEnabled = soundEnabled;
    }

    public UserInfo getUserInfo() {
        return userInfo;
    }
}
