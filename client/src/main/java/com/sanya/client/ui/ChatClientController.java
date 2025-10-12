package com.sanya.client.ui;

import com.sanya.Message;
import com.sanya.client.ApplicationContext;

public class ChatClientController {

    private UIFacade ui;
    private final ApplicationContext context;

    public ChatClientController(ApplicationContext context) {
        this.context = context;
    }

    public void setUIFacade(UIFacade ui) {
        this.ui = ui;
    }

    // 👇 Обработчик отправки сообщения
    public void onSendMessage(String text) {
        System.out.println("[Controller] Sending message: " + text);
        // Временно просто выводим, позже сюда добавится eventBus.publish(new MessageSendEvent(text))
        if (ui != null) ui.displayNotification("Вы: " + text);
    }

    // 👇 Обработчик выбора пользователя
    public void onUserSelected(String username) {
        System.out.println("[Controller] Selected user: " + username);
        if (ui != null) ui.displayNotification("Вы выбрали пользователя: " + username);
    }

    // 👇 Обработчик входящих сообщений
    public void onReceiveMessage(Message msg) {
        System.out.println("[Controller] Message received: " + msg);
        if (ui != null)
            ui.displayNotification(msg.getFrom() + ": " + msg.getText());
    }
}
