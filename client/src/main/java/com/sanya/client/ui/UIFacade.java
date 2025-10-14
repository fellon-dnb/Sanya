package com.sanya.client.ui;
import java.io.File;
import java.util.List;
public interface UIFacade {
    //  Сообщения
    void appendChatMessage(String text);
    void appendSystemMessage(String text);
    void clearChat();

    //  Пользователи
    void updateUserList(List<String> usernames);

    // Файлы
    void showFileTransferProgress(String filename, int percent, boolean outgoing);
    void showFileTransferCompleted(String filename, boolean outgoing);
    File askFileToSend();
    void showFileSaveDialog(String filename, byte[] data);

    //  Голосовые сообщения
    void showVoiceMessage(String username, byte[] data);
    void showVoiceRecordingStatus(boolean recording);

    //  Уведомления и ошибки
    void showInfo(String message);
    void showWarning(String message);
    void showError(String message);

    //  Темы и настройки
    void applyTheme(Object theme);
}

