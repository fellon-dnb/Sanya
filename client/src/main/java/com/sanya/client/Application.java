package com.sanya.client;

import com.ancevt.replines.core.argument.Arguments;
import com.sanya.client.settings.NetworkSettings;
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
            // === Ввод имени ===
            String username = usernameFromCli.isEmpty()
                    ? JOptionPane.showInputDialog("Enter your Name:")
                    : usernameFromCli;

            if (username == null || username.isBlank()) username = "Anonymous";
            ctx.getUserSettings().setName(username);

            // === Создание UI ===
            ChatClientUI ui = new ChatClientUI(ctx);

            // === Инициализация фасада ===
            UIFacade facade = new SwingUIFacade(ui);
            ctx.setUIFacade(facade);

            // === Контроллер (события UI ↔ логика) ===
            new ChatClientController(ctx);

            // === Сетевое подключение ===
            ChatClientConnector connector = new ChatClientConnector(
                    networkSettings.getHost(),
                    networkSettings.getPort(),
                    ctx.getUserSettings().getName(),
                    ctx.getEventBus()
            );

            // Привязка коннектора к ChatService
            ctx.services().chat().attachConnector(connector);

            connector.connect();

            // === Запуск UI ===
            ui.setVisible(true);
        });
    }

    public static void main(String[] args) {
        new Application().start(Arguments.parse(args));
    }
}
