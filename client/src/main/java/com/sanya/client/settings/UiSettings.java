package com.sanya.client.settings;

import com.sanya.events.system.Theme;

/**
 * UiSettings — класс для хранения пользовательских настроек интерфейса.
 * Управляет темой оформления и звуковыми уведомлениями.
 *
 * Назначение:
 *  - Сохранять предпочтения пользователя по теме и звуку.
 *  - Использоваться при инициализации UI и применении тем.
 *
 * Использование:
 *  UiSettings ui = new UiSettings();
 *  ui.setTheme(Theme.LIGHT);
 *  if (ui.isSoundEnabled()) playNotification();
 */
public final class UiSettings {

    /** Текущая тема интерфейса (по умолчанию — DARK). */
    private Theme theme = Theme.DARK;

    /** Флаг включения звуковых уведомлений. */
    private boolean soundEnabled = true;

    /** Возвращает текущую тему оформления. */
    public Theme getTheme() {
        return theme;
    }

    /** Устанавливает тему оформления. */
    public void setTheme(Theme theme) {
        if (theme == null) throw new IllegalArgumentException("Theme cannot be null");
        this.theme = theme;
    }

    /** Проверяет, включены ли звуковые уведомления. */
    public boolean isSoundEnabled() {
        return soundEnabled;
    }

    /** Включает или отключает звуковые уведомления. */
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
