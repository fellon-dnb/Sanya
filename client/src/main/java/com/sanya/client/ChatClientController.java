package com.sanya.client;

import com.sanya.client.ui.UIFacade;
import com.sanya.events.MessageSendEvent;

import javax.swing.*;

/**
 * Упрощенный контроллер - основная логика подписок вынесена в EventSubscriptionsManager.
 * Теперь отвечает только за специфичную UI логику и обработку команд.
 */
public class ChatClientController {

    private final ApplicationContext context;
    private final UIFacade ui;

    public ChatClientController(ApplicationContext context) {
        this.context = context;
        this.ui = context.getUIFacade();

        System.out.println("[ChatClientController] Controller initialized");
        // Все подписки на события теперь управляются через EventSubscriptionsManager
        // Этот класс может быть использован для UI-специфичной логики, не связанной с событиями
    }

    /**
     * Отправка текстового сообщения
     * Этот метод может вызываться из UI компонентов
     */
    public void sendMessage(String text) {
        if (text == null || text.isBlank()) {
            return;
        }

        try {
            // Публикуем событие отправки сообщения
            context.getEventBus().publish(new MessageSendEvent(text));

            // Локально добавляем сообщение в чат (дублирование убрано в EventSubscriptionsManager)
            // ui.appendChatMessage("Я: " + text);

        } catch (Exception e) {
            SwingUtilities.invokeLater(() ->
                    ui.showError("Ошибка отправки сообщения: " + e.getMessage()));
        }
    }

    /**
     * Очистка чата
     */
    public void clearChat() {
        try {
            context.services().chat().clearChat();
        } catch (Exception e) {
            SwingUtilities.invokeLater(() ->
                    ui.showError("Ошибка очистки чата: " + e.getMessage()));
        }
    }

    /**
     * Запуск записи голосового сообщения
     */
    public void startVoiceRecording() {
        try {
            context.services().voice().startRecording();
        } catch (Exception e) {
            SwingUtilities.invokeLater(() ->
                    ui.showError("Ошибка начала записи: " + e.getMessage()));
        }
    }

    /**
     * Остановка записи голосового сообщения
     */
    public void stopVoiceRecording() {
        try {
            context.services().voice().stopRecording();
        } catch (Exception e) {
            SwingUtilities.invokeLater(() ->
                    ui.showError("Ошибка остановки записи: " + e.getMessage()));
        }
    }

    /**
     * Отправка файла
     */
    public void sendFile(java.io.File file) {
        if (file == null || !file.exists()) {
            return;
        }

        new Thread(() -> {
            try {
                com.sanya.client.files.FileSender.sendFile(
                        file,
                        context.getUserSettings().getName(),
                        context.services().chat().getOutputStream(),
                        context.getEventBus()
                );
            } catch (Exception ex) {
                context.getEventBus().publish(
                        new com.sanya.events.SystemMessageEvent("[ERROR] Отправка файла: " + ex.getMessage())
                );
            }
        }, "FileSenderThread").start();
    }

    /**
     * Выход из приложения
     */
    public void exitApplication() {
        try {
            // Очищаем подписки
            if (context.getEventSubscriptionsManager() != null) {
                context.getEventSubscriptionsManager().unsubscribeAll();
            }

            // Закрываем соединение
            if (context.services().chat().getOutputStream() != null) {
                // Можно отправить сообщение о выходе, если нужно
            }

            System.out.println("[ChatClientController] Application exit requested");
            System.exit(0);

        } catch (Exception e) {
            System.err.println("Error during application exit: " + e.getMessage());
            System.exit(1);
        }
    }
}