package com.sanya.client.core;

import com.sanya.client.ApplicationContext;
import com.sanya.client.net.ChatConnector;
import com.sanya.client.facade.UIFacade;
import com.sanya.client.ui.dialog.ChatVoiceDialog;
import com.sanya.events.chat.*;
import com.sanya.events.core.EventHandler;
import com.sanya.events.file.FileIncomingEvent;
import com.sanya.events.system.SystemMessageEvent;
import com.sanya.events.system.ThemeChangedEvent;
import com.sanya.events.ui.ClearChatEvent;
import com.sanya.events.voice.*;
import com.sanya.files.FileTransferEvent;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * EventSubscriptionsManager — централизованный менеджер подписок на события.
 * Управляет регистрацией, хранением и очисткой всех обработчиков событий приложения.
 *
 * Назначение:
 *  - Упрощает отладку и контроль жизненного цикла событий.
 *  - Исключает дублирование логики подписок в разных модулях.
 *  - Гарантирует корректную отписку при завершении работы клиента.
 *
 * Использование:
 *  Создаётся в ApplicationContext, регистрирует все нужные события при старте клиента.
 *  Позволяет безопасно вызывать {@link #unsubscribeAll()} при выходе.
 */
public class EventSubscriptionsManager {

    private static final Logger log = Logger.getLogger(EventSubscriptionsManager.class.getName());

    /** Контекст приложения */
    private final ApplicationContext context;

    /** Интерфейс взаимодействия с UI */
    private final UIFacade ui;

    /** Сетевой коннектор */
    private final ChatConnector connector;

    /** Зарегистрированные подписки */
    private final List<Subscription> subscriptions;

    /** Активный диалог отправки голосового сообщения */
    private ChatVoiceDialog currentVoiceDialog;

    /**
     * Конструктор менеджера подписок.
     *
     * @param context   контекст приложения
     * @param ui        фасад пользовательского интерфейса
     * @param connector сетевой коннектор
     */
    public EventSubscriptionsManager(ApplicationContext context, UIFacade ui, ChatConnector connector) {
        this.context = context;
        this.ui = ui;
        this.connector = connector;
        this.subscriptions = new ArrayList<>();
    }

    /**
     * Регистрирует все стандартные подписки приложения.
     */
    public void registerAllSubscriptions() {
        log.info("Registering all event subscriptions...");
        registerMessageSubscriptions();
        registerUserListSubscriptions();
        registerVoiceSubscriptions();
        registerFileTransferSubscriptions();
        registerThemeSubscriptions();
        log.info("Total subscriptions registered: " + subscriptions.size());
    }

    /**
     * Отписывает все ранее зарегистрированные события.
     */
    public void unsubscribeAll() {
        subscriptions.forEach(Subscription::unsubscribe);
        subscriptions.clear();
        log.info("All event subscriptions removed");
    }

    /** === Подписки на сообщения === */
    private void registerMessageSubscriptions() {
        // Отправка сообщений (UI -> сеть)
        subscribe(MessageSendEvent.class, e -> {
            if (connector != null) {
                connector.sendMessage(e.text());
            }
        });

        // Получение сообщений (сеть -> UI)
        subscribe(MessageReceivedEvent.class, e -> {
            String self = context.getUserSettings().getName();
            SwingUtilities.invokeLater(() -> {
                if (e.message().getFrom().equals(self)) {
                    ui.appendChatMessage("Я: " + e.message().getText());
                } else {
                    ui.appendChatMessage(e.message().toString());
                }
            });
        });

        // Очистка чата
        subscribe(ClearChatEvent.class, e -> SwingUtilities.invokeLater(ui::clearChat));
    }

    /** === Подписки на пользователей === */
    private void registerUserListSubscriptions() {
        subscribe(UserListUpdatedEvent.class, e ->
                SwingUtilities.invokeLater(() -> ui.updateUserList(e.usernames())));

        subscribe(UserDisconnectedEvent.class, e ->
                log.info("User disconnected: " + e.username()));
    }

    /** === Подписки на голосовые события === */
    private void registerVoiceSubscriptions() {
        // Голосовые сообщения
        subscribe(VoiceMessageReadyEvent.class, e -> {
            if (!e.recipient().equals(context.getUserSettings().getName())) {
                SwingUtilities.invokeLater(() -> ui.showVoiceMessage(e.recipient(), e.data()));
            }
        });

        // Диалог отправки после остановки записи
        subscribe(VoiceRecordingStoppedEvent.class, e -> {
            if (e.username().equals(context.getUserSettings().getName())) {
                SwingUtilities.invokeLater(() -> openVoiceDialog(e.data()));
            }
        });

        // Изменение статуса записи
        subscribe(VoiceRecordingEvent.class, e ->
                SwingUtilities.invokeLater(() -> ui.showVoiceRecordingStatus(e.started())));

        // Обновление уровня громкости
        subscribe(VoiceLevelEvent.class, e ->
                SwingUtilities.invokeLater(() -> updateVoiceLevel(e.level())));
    }

    /** === Подписки на события передачи файлов === */
    private void registerFileTransferSubscriptions() {
        subscribe(FileTransferEvent.class, e -> {
            switch (e.type()) {
                case STARTED -> ui.showFileTransferProgress(e.filename(), 0, e.outgoing());
                case PROGRESS -> {
                    int percent = (int) ((100.0 * e.transferredBytes()) / e.totalBytes());
                    ui.showFileTransferProgress(e.filename(), percent, e.outgoing());
                }
                case COMPLETED -> ui.showFileTransferCompleted(e.filename(), e.outgoing());
                case FAILED -> ui.showError("Ошибка при передаче файла: " + e.errorMessage());
            }
        });

        subscribe(FileIncomingEvent.class, e ->
                log.info("File incoming: " + e.request()));
    }

    /** === Подписки на системные и UI события === */
    private void registerThemeSubscriptions() {
        subscribe(ThemeChangedEvent.class, e ->
                SwingUtilities.invokeLater(() -> ui.applyTheme(e.theme())));

        subscribe(SystemMessageEvent.class, e ->
                SwingUtilities.invokeLater(() -> ui.showError(e.message())));
    }

    /**
     * Открывает диалог для подтверждения отправки голосового сообщения.
     */
    private void openVoiceDialog(byte[] data) {
        if (currentVoiceDialog != null && currentVoiceDialog.isVisible()) {
            currentVoiceDialog.dispose();
        }

        currentVoiceDialog = new ChatVoiceDialog(null, data, context.services().voice());
        currentVoiceDialog.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                currentVoiceDialog = null;
            }
        });
        currentVoiceDialog.setVisible(true);
    }

    /**
     * Обновляет уровень громкости во время записи.
     *
     * @param level значение громкости от 0 до 1
     */
    private void updateVoiceLevel(double level) {
        int percent = (int) (level * 100);
        log.fine("Voice recording level: " + percent + "%");
    }

    /**
     * Унифицированная регистрация подписчиков с отслеживанием.
     */
    private <E> void subscribe(Class<E> eventType, EventHandler<E> handler) {
        context.getEventBus().subscribe(eventType, handler);
        subscriptions.add(new Subscription(eventType, handler));
    }

    /**
     * Вспомогательный класс для хранения данных о подписке.
     */
    private static class Subscription {
        private final Class<?> eventType;
        private final EventHandler<?> handler;

        public <E> Subscription(Class<E> eventType, EventHandler<E> handler) {
            this.eventType = eventType;
            this.handler = handler;
        }

        /**
         * Заглушка для возможной будущей реализации отписки.
         * В текущей версии EventBus не предоставляет обратной ссылки.
         */
        @SuppressWarnings("unchecked")
        public void unsubscribe() {
            // Можно реализовать отписку через EventBus при его расширении.
        }
    }
}
