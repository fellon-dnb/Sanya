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
 * Application — точка входа клиентского приложения Sanya Chat.
 * Отвечает за инициализацию контекста, сервисов, GUI и сетевого взаимодействия.
 *
 * Назначение:
 * - Настроить среду клиента (DI, EventBus, шифрование, UI, соединение).
 * - Запустить основной цикл приложения и отреагировать на сетевые события.
 *
 * Архитектура запуска:
 * 1. Чтение аргументов командной строки.
 * 2. Инициализация контекста и криптографии.
 * 3. Создание UI и регистрация сервисов.
 * 4. Подключение к серверу через {@link ChatConnector}.
 * 5. Управление событиями через {@link EventSubscriptionsManager}.
 * 6. Безопасное завершение с очисткой ресурсов.
 *
 * Пример запуска:
 * java -jar sanya-client.jar --host localhost --port 12345 --username Alice
 */
public final class Application {

    private static final Logger log = Logger.getLogger(Application.class.getName());

    /**
     * Точка старта клиента. Инициализирует компоненты и запускает UI.
     *
     * @param args аргументы командной строки, поддерживает:
     *             --host, -h (адрес сервера),
     *             --port, -p (порт),
     *             --username, -u (имя пользователя)
     */
    public void start(Arguments args) {
        // Настройка сети
        NetworkSettings networkSettings = new NetworkSettings(
                args.get(String.class, new String[]{"--host", "-h"}, "localhost"),
                args.get(Integer.class, new String[]{"--port", "-p"}, 12345)
        );

        // Имя пользователя
        String usernameFromCli = args.get(String.class, new String[]{"--username", "-u"}, "");

        // Создание контекста
        ApplicationContext ctx = new ApplicationContext(networkSettings);

        // Инициализация криптографии
        var keyDir = new KeyDirectory();
        var encryptor = new Encryptor(keyDir);

        ctx.di().registerSingleton(KeyDirectory.class, () -> keyDir);
        ctx.di().registerSingleton(Encryptor.class, () -> encryptor);

        // Запуск UI в EDT
        SwingUtilities.invokeLater(() -> {
            try {
                // Имя пользователя
                String username = usernameFromCli.isEmpty()
                        ? JOptionPane.showInputDialog("Enter your Name:")
                        : usernameFromCli;

                if (username == null || username.isBlank()) username = "Anonymous";
                ctx.getUserSettings().setName(username);

                // Инициализация UI
                ChatClientUI ui = new ChatClientUI(ctx);
                UIFacade facade = new SwingUIFacade(ctx, ui.getMainPanel());
                ctx.setUIFacade(facade);

                // Создание сетевого коннектора
                ChatConnector connector = new ChatConnector(
                        ctx,
                        networkSettings.getHost(),
                        networkSettings.getPort(),
                        ctx.getUserSettings().getName(),
                        ctx.getEventBus(),
                        keyDir,
                        encryptor
                );
                ctx.di().registerSingleton(ChatConnector.class, () -> connector);

                // Подключение чата к транспортному уровню
                ctx.services().chat().attachOutputSupplier(connector::isConnected, connector::sendObject);

                // Регистрация подписок на события
                EventSubscriptionsManager subscriptionsManager = new EventSubscriptionsManager(ctx, facade, connector);
                ctx.setEventSubscriptionsManager(subscriptionsManager);
                subscriptionsManager.registerAllSubscriptions();

                // Реакция на потерю соединения
                ctx.getEventBus().subscribe(ConnectionLostEvent.class, e ->
                        ctx.getUIFacade().showWarning("[NETWORK] " + e.reason() +
                                (e.willReconnect() ? " (reconnecting...)" : "")));

                // Контроллер UI
                new ChatClientController(ctx);

                // Подключение к серверу
                connector.connect();

                ui.setVisible(true);

                // Безопасное завершение
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

    /**
     * Главная точка входа программы.
     * Выполняет настройку логирования и инициализирует запуск клиента.
     */
    public static void main(String[] args) {
        // Настройка логгера
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

        // Запуск приложения
        try {
            new Application().start(Arguments.parse(args));
        } catch (Exception e) {
            log.severe("Fatal error starting application: " + e.getMessage());
            log.log(Level.SEVERE, "Stack trace:", e);
            System.exit(1);
        }
    }
}
