package com.sanya.client.facade.swing;

import com.sanya.client.ApplicationContext;
import com.sanya.client.facade.UIFacade;
import com.sanya.client.ui.main.ChatMainPanel;
import com.sanya.client.ui.NotificationManager;
import com.sanya.events.system.SystemMessageEvent;
import com.sanya.events.system.Theme;
import com.sanya.events.system.SystemInfoEvent;

import javax.swing.*;
import java.io.File;
import java.util.List;

/**
 * SwingUIFacade — реализация интерфейса {@link UIFacade} для Swing-клиента.
 * Инкапсулирует логику обновления интерфейса и отображения уведомлений.
 *
 * Назначение:
 *  - Изолировать прямые вызовы Swing-компонентов от бизнес-логики.
 *  - Обеспечить безопасные обновления UI из разных потоков.
 *  - Поддерживать единообразное поведение для всех пользовательских уведомлений.
 *
 * Использование:
 *  Создаётся в {@link com.sanya.client.ui.ChatClientUI} и регистрируется в {@link ApplicationContext}.
 *  Все модули приложения обращаются к UI через этот фасад.
 */
public class SwingUIFacade implements UIFacade {

    /** Контекст приложения (для доступа к EventBus и настройкам) */
    private final ApplicationContext ctx;

    /** Основная панель пользовательского интерфейса */
    private final ChatMainPanel mainPanel;

    /**
     * Конструктор фасада.
     * Подписывает UI на системные события и связывает NotificationManager.
     *
     * @param ctx        контекст приложения
     * @param mainPanel  главная панель чата
     */
    public SwingUIFacade(ApplicationContext ctx, ChatMainPanel mainPanel) {
        this.ctx = ctx;
        this.mainPanel = mainPanel;

        ctx.getEventBus().subscribe(SystemMessageEvent.class,
                e -> NotificationManager.showError(e.message()));
        ctx.getEventBus().subscribe(SystemInfoEvent.class,
                e -> NotificationManager.showInfo(e.message()));
    }

    /** Добавляет обычное сообщение в чат. */
    @Override
    public void appendChatMessage(String text) {
        mainPanel.appendChatMessage(text);
    }

    /** Добавляет системное сообщение в чат. */
    @Override
    public void appendSystemMessage(String text) {
        mainPanel.appendSystemMessage("[SYSTEM] " + text);
    }

    /** Очищает чат. */
    @Override
    public void clearChat() {
        SwingUtilities.invokeLater(mainPanel::clearChat);
    }

    /** Обновляет список пользователей в UI. */
    @Override
    public void updateUserList(List<String> usernames) {
        SwingUtilities.invokeLater(() -> mainPanel.updateUserList(usernames));
    }

    /** Отображает прогресс передачи файла. */
    @Override
    public void showFileTransferProgress(String filename, int percent, boolean outgoing) {
        mainPanel.updateFileTransferProgress(filename, percent, outgoing);
    }

    /** Отображает завершение передачи файла. */
    @Override
    public void showFileTransferCompleted(String filename, boolean outgoing) {
        mainPanel.fileTransferCompleted(filename, outgoing);
    }

    /** Показывает диалог выбора файла для отправки. */
    @Override
    public File askFileToSend() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            return chooser.getSelectedFile();
        }
        return null;
    }

    /** Заглушка для диалога сохранения файла. */
    @Override
    public void showFileSaveDialog(String filename, byte[] data) {
        // TODO: реализовать сохранение файла на диск
    }

    /** Добавляет кнопку для воспроизведения голосового сообщения. */
    @Override
    public void showVoiceMessage(String username, byte[] data) {
        mainPanel.addVoiceMessage(username, data);
    }

    /** Отображает индикатор записи голоса. */
    @Override
    public void showVoiceRecordingStatus(boolean recording) {
        mainPanel.setRecordingIndicator(recording);
    }

    /** Показывает информационное уведомление. */
    @Override
    public void showInfo(String message) {
        NotificationManager.showInfo(message);
    }

    /** Показывает предупреждение. */
    @Override
    public void showWarning(String message) {
        NotificationManager.showWarning(message);
    }

    /** Показывает сообщение об ошибке. */
    @Override
    public void showError(String message) {
        NotificationManager.showError(message);
    }

    /** Применяет выбранную тему оформления. */
    @Override
    public void applyTheme(Object theme) {
        if (theme instanceof Theme t) {
            mainPanel.applyTheme(t);
        }
    }
}
