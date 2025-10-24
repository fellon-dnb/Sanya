package com.sanya.client.settings;

/**
 * UserSettings — класс для хранения персональных настроек пользователя.
 * Содержит имя, используемое при подключении и отображении в чате.
 *
 * Назначение:
 *  - Инкапсулировать пользовательские данные.
 *  - Обеспечивать доступ к имени для сервисов (чата, аудио, событий и т. д.).
 *
 * Использование:
 *  UserSettings user = new UserSettings();
 *  user.setName("Sanya");
 *  String username = user.getName();
 */
public final class UserSettings {

    /** Имя пользователя, отображаемое в чате. */
    private String name;

    /** Возвращает имя пользователя. */
    public String getName() {
        return name;
    }

    /** Устанавливает имя пользователя. */
    public void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Username cannot be null or blank");
        }
        this.name = name;
    }

    @Override
    public String toString() {
        return "UserSettings{" +
                "name='" + name + '\'' +
                '}';
    }
}
