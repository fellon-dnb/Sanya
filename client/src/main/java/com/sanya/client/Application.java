package com.sanya.client;

import com.ancevt.replines.core.argument.Arguments;
import com.sanya.client.core.EventSubscriptionsManager;
import com.sanya.client.facade.UIFacade;
import com.sanya.client.facade.swing.SwingUIFacade;
import com.sanya.client.net.ChatConnector;
import com.sanya.client.security.Encryptor;
import com.sanya.client.security.KeyDirectory;
import com.sanya.client.settings.NetworkSettings;
import com.sanya.client.ui.ChatClientUI;
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
        // === Настройка сети ===
        NetworkSettings networkSettings = new NetworkSettings(
                args.get(String.class, new String[]{"--host", "-h"}, "localhost"),
                args.get(Integer.class, new String[]{"--port", "-p"}, 12345)
        );

        // === Имя пользователя из CLI ===
        String usernameFromCli = args.get(String.class, new String[]{"--username", "-u"}, "");

        // === Контекст приложения ===
        ApplicationContext ctx = new ApplicationContext(networkSettings);

        // === Инициализация криптографии ===
        var keyDir = new KeyDirectory();
        var encryptor = new Encryptor(keyDir);

        // Регистрируем в DI
        ctx.di().registerSingleton(KeyDirectory.class, () -> keyDir);
        ctx.di().registerSingleton(Encryptor.class, () -> encryptor);

        // === UI и запуск ===
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

                // === Создаём ChatConnector с поддержкой шифрования ===
                ChatConnector connector = new ChatConnector(
                        ctx,
                        networkSettings.getHost(),
                        networkSettings.getPort(),
                        ctx.getUserSettings().getName(),
                        ctx.getEventBus(),
                        keyDir,
                        encryptor
                );

                // Регистрируем коннектор
                ctx.di().registerSingleton(ChatConnector.class, () -> connector);

                // Подключаем ChatService к коннектору
                ctx.services().chat().attachOutputSupplier(connector::isConnected, connector::sendObject);

                // === Подписки ===
                EventSubscriptionsManager subscriptionsManager = new EventSubscriptionsManager(ctx, facade, connector);
                ctx.setEventSubscriptionsManager(subscriptionsManager);
                subscriptionsManager.registerAllSubscriptions();

                // === Реакция на потерю соединения ===
                ctx.getEventBus().subscribe(ConnectionLostEvent.class, e ->
                        ctx.getUIFacade().showWarning("[NETWORK] " + e.reason() +
                                (e.willReconnect() ? " (reconnecting...)" : "")));

                // === Контроллер UI ===
                new ChatClientController(ctx);

                // === Запуск соединения ===
                connector.connect();

                ui.setVisible(true);

                // === Завершение работы ===
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    log.info("Shutdown hook executed");
                    if (ctx.getEventSubscriptionsManager() != null)
                        ctx.getEventSubscriptionsManager().unsubscribeAll();
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
        try {
            java.nio.file.Files.createDirectories(java.nio.file.Path.of("logs"));
            try (InputStream input = Application.class.getResourceAsStream("/logging.properties")) {
                if (input != null) {
                    LogManager.getLogManager().readConfiguration(input);
                    log.info("Logging configuration loaded successfully");
                } else {
                    System.err.println("[WARN] logging.properties not found in resources");
                }
            }
        } catch (Exception e) {
            System.err.println("[WARN] Failed to initialize logging: " + e.getMessage());
        }

        // === Запуск приложения ===
        try {
            new Application().start(Arguments.parse(args));
        } catch (Exception e) {
            log.severe("Fatal error starting application: " + e.getMessage());
            log.log(Level.SEVERE, "Stack trace:", e);
            System.exit(1);
        }
    }
}
