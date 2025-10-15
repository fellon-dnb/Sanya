package com.sanya.client.ui.theme;

import com.sanya.client.ApplicationContext;
import com.sanya.events.EventBus;
import com.sanya.events.Theme;
import com.sanya.events.ThemeChangedEvent;

/**
 * Менеджер темы — централизованное управление активной темой приложения.
 */
public final class ThemeManager {
    private  final  ApplicationContext ctx;
    private final EventBus bus;
    private Theme current = Theme.LIGHT;

    public ThemeManager(ApplicationContext ctx, EventBus bus) {
        this.ctx = ctx;
        this.bus = bus;
        this.current = ctx.getUiSettings().getTheme();
    }

    public Theme getCurrent() {
        return current;
    }

    public void setTheme(Theme theme) {
        if (theme != current) {
            current = theme;
            ctx.getUiSettings().setTheme(theme); // <-- сохраняем в настройки
            bus.publish(new ThemeChangedEvent(theme));
        }
    }

    public void toggle() {
        setTheme(current == Theme.DARK ? Theme.LIGHT : Theme.DARK);
    }
}
