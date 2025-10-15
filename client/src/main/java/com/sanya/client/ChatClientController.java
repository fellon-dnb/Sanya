package com.sanya.client;

import com.sanya.client.ui.UIFacade;
import com.sanya.client.ui.dialog.ChatVoiceDialog;
import com.sanya.events.*;
import com.sanya.files.FileTransferEvent;

import javax.swing.*;

/**
 * Связывает EventBus и UI. Реагирует на события и вызывает нужные методы интерфейса.
 */
public class ChatClientController {

    private final ApplicationContext context;
    private final UIFacade ui;
    private ChatVoiceDialog currentVoiceDialog;

    public ChatClientController(ApplicationContext context) {
        this.context = context;
        this.ui = context.getUIFacade();

        EventBus eventBus = context.getEventBus();
        if (eventBus != null) {
            subscribeToEvents(eventBus);
        }
    }

    private void subscribeToEvents(EventBus eventBus) {

        eventBus.subscribe(MessageReceivedEvent.class, e -> {
            String self = context.getUserSettings().getName();
            if (e.message().getFrom().equals(self))
                ui.appendChatMessage("Я: " + e.message().getText());
            else
                ui.appendChatMessage(e.message().toString());
        });

        eventBus.subscribe(UserListUpdatedEvent.class, e -> ui.updateUserList(e.usernames()));
        eventBus.subscribe(ClearChatEvent.class, e -> ui.clearChat());

        // Голосовые сообщения - только от других пользователей
        eventBus.subscribe(VoiceMessageReadyEvent.class, e -> {
            if (!e.username().equals(context.getUserSettings().getName())) {
                SwingUtilities.invokeLater(() -> ui.showVoiceMessage(e.username(), e.data()));
            }
        });

        // Диалог после остановки записи - только для текущего пользователя
        eventBus.subscribe(VoiceRecordingStoppedEvent.class, e -> {
            if (!e.username().equals(context.getUserSettings().getName())) {
                return;
            }

            SwingUtilities.invokeLater(() -> {
                // Закрываем предыдущий диалог, если есть
                if (currentVoiceDialog != null && currentVoiceDialog.isVisible()) {
                    currentVoiceDialog.dispose();
                }

                // Создаем новый диалог
                currentVoiceDialog = new ChatVoiceDialog(
                        null,
                        e.data(),
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
            });
        });

        eventBus.subscribe(FileTransferEvent.class, e -> {
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

        eventBus.subscribe(VoiceRecordingEvent.class, e ->
                SwingUtilities.invokeLater(() ->
                        ui.showVoiceRecordingStatus(e.started())));
    }

    public void sendMessage(String text) {
        if (text == null || text.isBlank()) return;
        context.getEventBus().publish(new MessageSendEvent(text));
        ui.appendChatMessage("Я: " + text);
    }
}