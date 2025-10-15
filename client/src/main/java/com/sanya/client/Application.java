package com.sanya.client;

import com.ancevt.replines.core.argument.Arguments;
import com.sanya.client.core.EventSubscriptionsManager;
import com.sanya.client.net.ChatConnector;
import com.sanya.client.settings.NetworkSettings;
import com.sanya.client.ui.ChatClientUI;
import com.sanya.client.facade.UIFacade;
import com.sanya.client.facade.swing.SwingUIFacade;
import com.sanya.events.system.ConnectionLostEvent;

import javax.swing.*;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * Точка входа клиента чата.
 * Настраивает контекст, GUI и сетевое подключение.
 */
public class Application {
    private static final Logger log = Logger.getLogger(Application.class.getName());

    public void start(Arguments args) {
        NetworkSettings networkSettings = new NetworkSettings(
                args.get(String.class, new String[]{"--host", "-h"}, "localhost"),
                args.get(Integer.class, new String[]{"--port", "-p"}, 12345)
        );

        String usernameFromCli = args.get(String.class, new String[]{"--username", "-u"}, "");

        ApplicationContext ctx = new ApplicationContext(networkSettings);

        SwingUtilities.invokeLater(() -> {
            try {
                String username = usernameFromCli.isEmpty()
                        ? JOptionPane.showInputDialog("Enter your Name:")
                        : usernameFromCli;

                if (username == null || username.isBlank()) username = "Anonymous";
                ctx.getUserSettings().setName(username);

                ChatClientUI ui = new ChatClientUI(ctx);

                UIFacade facade = new SwingUIFacade(ctx, ui.getMainPanel());
                ctx.setUIFacade(facade);

                // === Новый ChatConnector ===
                ChatConnector connector = new ChatConnector(
                        networkSettings.getHost(),
                        networkSettings.getPort(),
                        ctx.getUserSettings().getName(),
                        ctx.getEventBus()
                );

// Регистрация коннектора в DI-контейнере
                ctx.di().registerSingleton(ChatConnector.class, () -> connector);

// Передаём ChatService логику отправки
                ctx.services().chat().attachOutputSupplier(connector::isConnected, connector::sendObject);

                // === Подписки ===
                EventSubscriptionsManager subscriptionsManager =
                        new EventSubscriptionsManager(ctx, facade, connector);
                ctx.setEventSubscriptionsManager(subscriptionsManager);
                subscriptionsManager.registerAllSubscriptions();

                // === Подписка на потерю соединения ===
                ctx.getEventBus().subscribe(ConnectionLostEvent.class, e ->
                        ctx.getUIFacade().showWarning("[NETWORK] " + e.reason() +
                                (e.willReconnect() ? " (reconnecting...)" : "")));

                new ChatClientController(ctx);

                connector.connect();
                ui.setVisible(true);

                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    log.info("Shutdown hook executed");
                    if (ctx.getEventSubscriptionsManager() != null) {
                        ctx.getEventSubscriptionsManager().unsubscribeAll();
                    }
                    connector.close();
                }));

            } catch (Exception e) {
                JOptionPane.showMessageDialog(null,
                        "Failed to start application: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                log.log(Level.SEVERE, "Startup failed", e);
                System.exit(1);
            }
        });
    }

    public static void main(String[] args) {
        // === Настройка логгера ===
        try (InputStream input = Application.class.getResourceAsStream("/logging.properties")) {
            if (input != null) {
                LogManager.getLogManager().readConfiguration(input);
                log.info("Logging configuration loaded successfully");
            } else {
                System.err.println("[WARN] logging.properties not found in resources");
            }
        } catch (Exception e) {
            System.err.println("[WARN] Failed to initialize logging: " + e.getMessage());
        }

        // === Запуск приложения ===
        try {
            new Application().start(Arguments.parse(args));
        } catch (Exception e) {
            log.severe("Fatal error starting application: " + e.getMessage());
            e.printStackTrace(); // опционально можно убрать, если все пишем в лог
            System.exit(1);
        }
    }
}
