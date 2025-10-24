package com.sanya.client.facade;

import java.io.File;
import java.util.List;

/**
 * UIFacade — универсальный интерфейс для взаимодействия между бизнес-логикой и пользовательским интерфейсом.
 * Определяет контракты для отображения сообщений, файлов, уведомлений и управления темами.
 *
 * Назначение:
 *  - Абстрагировать логику работы UI от конкретной реализации (Swing, JavaFX, Web и т.д.).
 *  - Обеспечить единый API для обновления интерфейса независимо от используемой технологии.
 *  - Облегчить тестирование и модульное разделение кода.
 *
 * Использование:
 *  Реализация для Swing расположена в пакете {@code com.sanya.client.facade.swing}.
 *  Все вызовы интерфейса выполняются из сервисов и менеджеров через {@link com.sanya.client.ApplicationContext}.
 */
public interface UIFacade {

    /** Добавляет обычное сообщение в чат. */
    void appendChatMessage(String text);

    /** Добавляет системное сообщение (например, уведомление сервера). */
    void appendSystemMessage(String text);

    /** Очищает окно чата. */
    void clearChat();

    /** Обновляет список активных пользователей. */
    void updateUserList(List<String> usernames);

    /**
     * Отображает прогресс передачи файла.
     *
     * @param filename имя файла
     * @param percent  процент выполнения
     * @param outgoing true, если отправка; false, если приём
     */
    void showFileTransferProgress(String filename, int percent, boolean outgoing);

    /**
     * Отображает завершение передачи файла.
     *
     * @param filename имя файла
     * @param outgoing true, если отправка; false, если приём
     */
    void showFileTransferCompleted(String filename, boolean outgoing);

    /** Открывает диалог выбора файла для отправки. */
    File askFileToSend();

    /** Отображает диалог сохранения полученного файла. */
    void showFileSaveDialog(String filename, byte[] data);

    /** Добавляет новое голосовое сообщение в чат. */
    void showVoiceMessage(String username, byte[] data);

    /** Отображает состояние записи (начало/окончание). */
    void showVoiceRecordingStatus(boolean recording);


    /** Показывает информационное уведомление. */
    void showInfo(String message);

    /** Показывает предупреждение. */
    void showWarning(String message);

    /** Показывает сообщение об ошибке. */
    void showError(String message);


    /** Применяет тему оформления к интерфейсу. */
    void applyTheme(Object theme);
}
