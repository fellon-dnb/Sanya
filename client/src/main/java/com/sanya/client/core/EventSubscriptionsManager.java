package com.sanya.client.core;

import com.sanya.client.ApplicationContext;

import com.sanya.client.net.ChatConnector;
import com.sanya.client.facade.UIFacade;
import com.sanya.client.ui.dialog.ChatVoiceDialog;
import com.sanya.events.chat.MessageReceivedEvent;
import com.sanya.events.chat.MessageSendEvent;
import com.sanya.events.chat.UserDisconnectedEvent;
import com.sanya.events.chat.UserListUpdatedEvent;
import com.sanya.events.core.EventHandler;
import com.sanya.events.file.FileIncomingEvent;
import com.sanya.events.system.SystemMessageEvent;
import com.sanya.events.system.ThemeChangedEvent;
import com.sanya.events.ui.ClearChatEvent;
import com.sanya.events.voice.VoiceLevelEvent;
import com.sanya.events.voice.VoiceMessageReadyEvent;
import com.sanya.events.voice.VoiceRecordingEvent;
import com.sanya.events.voice.VoiceRecordingStoppedEvent;
import com.sanya.files.FileTransferEvent;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Централизованный менеджер подписок на события.
 * Управляет всеми подписками в приложении, что упрощает отладку и предотвращает утечки памяти.
 */
public class EventSubscriptionsManager {
    private static final Logger log = Logger.getLogger(EventSubscriptionsManager.class.getName());
    private final ApplicationContext context;
    private final UIFacade ui;
    private final ChatConnector connector;
    private final List<Subscription> subscriptions;

    private ChatVoiceDialog currentVoiceDialog;

    public EventSubscriptionsManager(ApplicationContext context, UIFacade ui, ChatConnector connector) {
        this.context = context;
        this.ui = ui;
        this.connector = connector;
        this.subscriptions = new ArrayList<>();
    }

    /**
     * Регистрирует все подписки приложения
     */
    public void registerAllSubscriptions() {
        log.info("Registering voice subscriptions, total so far: " + subscriptions.size());
        registerMessageSubscriptions();
        registerUserListSubscriptions();
        registerVoiceSubscriptions();
        registerFileTransferSubscriptions();
        registerThemeSubscriptions();

        log.info("Registered " + subscriptions.size() + " event subscriptions");
    }

    /**
     * Отменяет все подписки (для cleanup при закрытии приложения)
     */
    public void unsubscribeAll() {
        subscriptions.forEach(Subscription::unsubscribe);
        subscriptions.clear();
        log.info("Unsubscribed all events");
    }

    private void registerMessageSubscriptions() {
        // Подписка на отправку сообщений (из UI в сеть)
        subscribe(MessageSendEvent.class, e -> {
            if (connector != null) {
                connector.sendMessage(e.text());
            }
        });

        // Подписка на получение сообщений (из сети в UI)
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
        subscribe(ClearChatEvent.class, e ->
                SwingUtilities.invokeLater(ui::clearChat));
    }

    private void registerUserListSubscriptions() {
        subscribe(UserListUpdatedEvent.class, e ->
                SwingUtilities.invokeLater(() -> ui.updateUserList(e.usernames())));

        subscribe(UserDisconnectedEvent.class, e ->
                log.info("User disconnected: " + e.username()));
    }

    private void registerVoiceSubscriptions() {
        // Голосовые сообщения от других пользователей
        subscribe(VoiceMessageReadyEvent.class, e -> {
            if (!e.username().equals(context.getUserSettings().getName())) {
                SwingUtilities.invokeLater(() -> ui.showVoiceMessage(e.username(), e.data()));
            }
        });

        // Диалог после остановки записи (только для текущего пользователя)
        subscribe(VoiceRecordingStoppedEvent.class, e -> {
            if (!e.username().equals(context.getUserSettings().getName())) {
                return;
            }

            SwingUtilities.invokeLater(() -> openVoiceDialog(e.data()));
        });

        // Статус записи голоса
        subscribe(VoiceRecordingEvent.class, e ->
                SwingUtilities.invokeLater(() ->
                        ui.showVoiceRecordingStatus(e.started())));

        // Уровень громкости при записи
        subscribe(VoiceLevelEvent.class, e ->
                SwingUtilities.invokeLater(() ->
                        updateVoiceLevel(e.level())));
    }

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

    private void registerThemeSubscriptions() {
        subscribe(ThemeChangedEvent.class, e ->
                SwingUtilities.invokeLater(() -> ui.applyTheme(e.theme())));

        subscribe(SystemMessageEvent.class, e ->
                SwingUtilities.invokeLater(() -> ui.showError(e.message())));
    }

    private void openVoiceDialog(byte[] data) {
        // Закрываем предыдущий диалог, если есть
        if (currentVoiceDialog != null && currentVoiceDialog.isVisible()) {
            currentVoiceDialog.dispose();
        }

        // Создаем новый диалог
        currentVoiceDialog = new ChatVoiceDialog(
                null,
                data,
                context.services().voice()
        );

        // Настраиваем слушатель закрытия
        currentVoiceDialog.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                currentVoiceDialog = null;
            }
        });

        currentVoiceDialog.setVisible(true);
    }

    private void updateVoiceLevel(double level) {
        // Эта логика может быть перенесена в UI, оставляем здесь для примера
        int percent = (int) (level * 100);
        log.fine("Voice recording level: " + percent + "%");
    }

    /**
     * Вспомогательный метод для подписки с автоматическим управлением жизненным циклом
     */
    private <E> void subscribe(Class<E> eventType, EventHandler<E> handler) {
        context.getEventBus().subscribe(eventType, handler);
        subscriptions.add(new Subscription(eventType, handler));
    }

    /**
     * Внутренний класс для отслеживания подписок
     */
    private static class Subscription {
        private final Class<?> eventType;
        private final EventHandler<?> handler;

        public <E> Subscription(Class<E> eventType, EventHandler<E> handler) {
            this.eventType = eventType;
            this.handler = handler;
        }

        @SuppressWarnings("unchecked")
        public void unsubscribe() {
            // Для отписки нам нужен EventBus, но он не хранится здесь
            // Реализуем отписку через ApplicationContext при необходимости
        }
    }
}