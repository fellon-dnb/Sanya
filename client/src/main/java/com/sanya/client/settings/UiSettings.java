package com.sanya.client.settings;

import com.sanya.events.Theme;

public class UiSettings {

    private Theme theme = Theme.DARK;
    private boolean soundEnabled = true;

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

    @Override
    public String toString() {
        return "UiSettings{" +
                "theme=" + theme +
                ", soundEnabled=" + soundEnabled +
                '}';
    }
}
