package com.sanya.client.ui;

import com.sanya.Message;
import com.sanya.client.ApplicationContext;
import com.sanya.events.*;
import com.sanya.files.FileTransferEvent;

public class ChatClientController {

    private final ApplicationContext context;
    private UIFacade ui;

    public ChatClientController(ApplicationContext context) {
        this.context = context;

        // Подключаемся к EventBus
        EventBus eventBus = context.getEventBus();
        if (eventBus != null) {
            subscribeToEvents(eventBus);
        }
    }

    public void setUIFacade(UIFacade ui) {
        this.ui = ui;
    }

    // === Отправка сообщений ===
    public void onSendMessage(String text) {
        if (text == null || text.isBlank()) return;

        // Публикуем событие — это перехватит ChatClientConnector
        context.getEventBus().publish(new MessageSendEvent(text));

        // Добавляем сообщение в чат локально (чтобы сразу видеть своё)
        if (ui != null) {
            ui.appendChatMessage("Я: " + text);
        }
    }

    // === Подписка на события ===
    private void subscribeToEvents(EventBus eventBus) {

        // 📩 Входящие сообщения
        eventBus.subscribe(MessageReceivedEvent.class, e -> {
            Message msg = e.message();
            if (ui != null) {
                ui.appendChatMessage(msg.toString());
                SoundPlayer.playMessageSound();
            }
        });

        // 🧑‍🤝‍🧑 Обновление списка пользователей
        eventBus.subscribe(UserListUpdatedEvent.class, e -> {
            if (ui != null) ui.updateUserList(e.usernames());
        });

        // 🎨 Смена темы
        eventBus.subscribe(ThemeChangedEvent.class, e -> {
            if (ui != null) ui.setTheme(e.theme());
        });

        // 📂 Прогресс передачи файлов
        eventBus.subscribe(FileTransferEvent.class, e -> {
            if (ui != null && e.type() == FileTransferEvent.Type.PROGRESS) {
                int percent = (int) ((e.transferredBytes() * 100) / e.totalBytes());
                ui.showFileTransferProgress(e.filename(), percent);
            }
        });

        // ⚠️ Системные уведомления
        eventBus.subscribe(UserConnectedEvent.class, e -> {
            if (ui != null)
                ui.displayNotification(e.username() + " вошёл в чат");
        });

        eventBus.subscribe(UserDisconnectedEvent.class, e -> {
            if (ui != null)
                ui.displayNotification(e.username() + " покинул чат");
        });
    }
}
