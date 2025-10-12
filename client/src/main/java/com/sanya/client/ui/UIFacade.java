package com.sanya.client.ui;

import com.sanya.events.Theme;

import java.util.List;

public interface UIFacade {
    void start(); // Инициализация и запуск UI (открытие главного окна чата)
    void showChatWindow(); // Показать окно чата
    void updateUserList(List<String> users); // Обновить список пользователей в UI
    void displayNotification(String message); // Отобразить уведомление пользователю
    void appendChatMessage(String text);  // добавить сообщение в чат
    void setTheme(Theme theme);  // сменить тему
    void showFileTransferProgress(String filename, int percent);  // показать прогресс передачи файла
}
