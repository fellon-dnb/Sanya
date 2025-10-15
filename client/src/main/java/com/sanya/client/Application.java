package com.sanya.client;

import com.ancevt.replines.core.argument.Arguments;
import com.sanya.client.core.EventSubscriptionsManager;
import com.sanya.client.settings.NetworkSettings;
import com.sanya.client.ui.ChatClientUI;
import com.sanya.client.ui.UIFacade;
import com.sanya.client.ui.swing.SwingUIFacade;

import javax.swing.*;

/**
 * Точка входа клиента чата.
 * Настраивает контекст, GUI и сетевое подключение.
 */
public class Application {

    public void start(Arguments args) {
        // === Чтение аргументов ===
        NetworkSettings networkSettings = new NetworkSettings(
                args.get(String.class, new String[]{"--host", "-h"}, "localhost"),
                args.get(Integer.class, new String[]{"--port", "-p"}, 12345)
        );

        String usernameFromCli = args.get(String.class, new String[]{"--username", "-u"}, "");

        // === Создание контекста ===
        ApplicationContext ctx = new ApplicationContext(networkSettings);

        SwingUtilities.invokeLater(() -> {
            try {
                // === Ввод имени ===
                String username = usernameFromCli.isEmpty()
                        ? JOptionPane.showInputDialog("Enter your Name:")
                        : usernameFromCli;

                if (username == null || username.isBlank()) username = "Anonymous";
                ctx.getUserSettings().setName(username);

                // === Создание UI ===
                ChatClientUI ui = new ChatClientUI(ctx);

                // === Инициализация фасада ===
                UIFacade facade = new SwingUIFacade(ctx, ui.getMainPanel());
                ctx.setUIFacade(facade);

                // === Сетевое подключение ===
                ChatClientConnector connector = new ChatClientConnector(
                        networkSettings.getHost(),
                        networkSettings.getPort(),
                        ctx.getUserSettings().getName(),
                        ctx.getEventBus()
                );

                // Привязка коннектора к ChatService
                ctx.services().chat().attachConnector(connector);

                // === Инициализация менеджера подписок ===
                EventSubscriptionsManager subscriptionsManager =
                        new EventSubscriptionsManager(ctx, facade, connector);
                ctx.setEventSubscriptionsManager(subscriptionsManager);

                // Регистрация всех подписок
                subscriptionsManager.registerAllSubscriptions();

                // === Упрощенный контроллер ===
                new ChatClientController(ctx);

                // === Подключение к серверу ===
                connector.connect();

                // === Запуск UI ===
                ui.setVisible(true);

                // === Shutdown hook для cleanup ===
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    System.out.println("[Application] Shutdown hook executed");
                    if (ctx.getEventSubscriptionsManager() != null) {
                        ctx.getEventSubscriptionsManager().unsubscribeAll();
                    }
                    if (connector != null) {
                        connector.close();
                    }
                }));

            } catch (Exception e) {
                JOptionPane.showMessageDialog(null,
                        "Failed to start application: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
                System.exit(1);
            }
        });
    }

    public static void main(String[] args) {
        try {
            new Application().start(Arguments.parse(args));
        } catch (Exception e) {
            System.err.println("Fatal error starting application: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}