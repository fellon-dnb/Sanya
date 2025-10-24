package com.sanya.client.ui.theme;

import com.sanya.client.ApplicationContext;
import com.sanya.client.core.api.EventBus;
import com.sanya.events.system.Theme;
import com.sanya.events.system.ThemeChangedEvent;

/**
 * ThemeManager — менеджер управления темой интерфейса приложения.
 * Отвечает за хранение текущей темы и уведомление всех компонентов о её изменении.
 *
 * Назначение:
 * - Централизованно управлять текущей темой (светлая или тёмная).
 * - Сохранять выбор пользователя в настройках {@link com.sanya.client.settings.UiSettings}.
 * - Публиковать событие {@link ThemeChangedEvent} при смене темы.
 *
 * Использование:
 * ThemeManager theme = ctx.services().theme();
 * theme.toggle(); // переключить тему
 * theme.setTheme(Theme.DARK); // явно задать тему
 */
public final class ThemeManager {

    /** Контекст приложения. */
    private final ApplicationContext ctx;

    /** Шина событий для уведомления компонентов об изменении темы. */
    private final EventBus bus;

    /** Текущая активная тема интерфейса. */
    private Theme current;

    /**
     * Создаёт менеджер темы и инициализирует его из пользовательских настроек.
     *
     * @param ctx контекст приложения
     * @param bus шина событий
     */
    public ThemeManager(ApplicationContext ctx, EventBus bus) {
        this.ctx = ctx;
        this.bus = bus;
        this.current = ctx.getUiSettings().getTheme();
    }

    /** Возвращает текущую активную тему. */
    public Theme getCurrent() {
        return current;
    }

    /**
     * Устанавливает новую тему интерфейса.
     * При изменении публикует событие {@link ThemeChangedEvent}
     * и сохраняет выбранную тему в пользовательских настройках.
     *
     * @param theme новая тема интерфейса
     */
    public void setTheme(Theme theme) {
        if (theme != current) {
            current = theme;
            ctx.getUiSettings().setTheme(theme);
            bus.publish(new ThemeChangedEvent(theme));
        }
    }

    /**
     * Переключает тему между светлой и тёмной.
     * Если активна {@link Theme#DARK}, устанавливает {@link Theme#LIGHT}, и наоборот.
     */
    public void toggle() {
        setTheme(current == Theme.DARK ? Theme.LIGHT : Theme.DARK);
    }
}
