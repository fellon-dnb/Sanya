package com.sanya.client;

import com.sanya.client.facade.UIFacade;
import com.sanya.events.chat.MessageSendEvent;
import com.sanya.events.system.SystemMessageEvent;

import javax.swing.*;

/**
 * ChatClientController — контроллер верхнего уровня, отвечающий за логику взаимодействия
 * между пользовательским интерфейсом и сервисами приложения.
 *
 * Назначение:
 * - Обрабатывать пользовательские действия, инициированные из UI (отправка сообщений, запись голоса, передача файлов).
 * - Делегировать бизнес-логику в сервисы, зарегистрированные в {@link ApplicationContext}.
 * - Выполнять визуальные уведомления и обработку ошибок, не связанных с EventBus.
 *
 * Архитектура:
 * Подписки на события вынесены в {@link com.sanya.client.core.EventSubscriptionsManager}.
 * Контроллер фокусируется на командах и действиях, инициированных пользователем.
 *
 * Потоковая модель:
 * Все операции, затрагивающие Swing-компоненты, выполняются через {@link SwingUtilities#invokeLater(Runnable)}.
 */
public final class ChatClientController {

    /** Глобальный контекст приложения. */
    private final ApplicationContext context;

    /** Фасад пользовательского интерфейса (для уведомлений и обновлений). */
    private final UIFacade ui;

    /**
     * Создаёт контроллер, привязанный к переданному контексту приложения.
     *
     * @param context экземпляр {@link ApplicationContext}, содержащий все сервисы клиента
     */
    public ChatClientController(ApplicationContext context) {
        this.context = context;
        this.ui = context.getUIFacade();
        System.out.println("[ChatClientController] Controller initialized");
    }

    /**
     * Отправляет текстовое сообщение через EventBus.
     * Может вызываться напрямую из компонентов UI.
     *
     * @param text содержимое сообщения
     */
    public void sendMessage(String text) {
        if (text == null || text.isBlank()) return;

        try {
            context.getEventBus().publish(new MessageSendEvent(text));
        } catch (Exception e) {
            SwingUtilities.invokeLater(() ->
                    ui.showError("Ошибка отправки сообщения: " + e.getMessage()));
        }
    }

    /**
     * Очищает окно чата.
     * Делегирует вызов сервису {@link com.sanya.client.service.ChatService}.
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
     * Начинает запись голосового сообщения.
     * Делегирует вызов сервису {@link com.sanya.client.service.audio.VoiceService}.
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
     * Завершает запись голосового сообщения.
     * Делегирует вызов сервису {@link com.sanya.client.service.audio.VoiceService}.
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
     * Отправляет выбранный файл на сервер в отдельном потоке.
     *
     * @param file файл для передачи
     */
    public void sendFile(java.io.File file) {
        if (file == null || !file.exists()) return;

        new Thread(() -> {
            try {
                context.services().fileSender().sendFile(
                        context.getUserSettings().getName(),
                        file,
                        context.services().chat()::sendObject
                );
            } catch (Exception ex) {
                context.getEventBus().publish(
                        new SystemMessageEvent("[ERROR] Отправка файла: " + ex.getMessage())
                );
            }
        }, "FileSenderThread").start();
    }

    /**
     * Выполняет безопасное завершение работы приложения.
     * Отписывает все события и корректно закрывает процесс.
     */
    public void exitApplication() {
        try {
            if (context.getEventSubscriptionsManager() != null) {
                context.getEventSubscriptionsManager().unsubscribeAll();
            }

            System.out.println("[ChatClientController] Application exit requested");
            System.exit(0);
        } catch (Exception e) {
            System.err.println("Error during application exit: " + e.getMessage());
            System.exit(1);
        }
    }
}
