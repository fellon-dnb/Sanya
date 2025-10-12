package com.sanya.client;

import com.ancevt.replines.core.argument.Arguments;
import com.sanya.client.commands.CommandHandler;
import com.sanya.client.ui.ChatClientController;
import com.sanya.client.ui.UIFacade;
import com.sanya.client.ui.swing.SwingUIFacade;
import com.sanya.events.EventBus;
import com.sanya.events.SimpleEventBus;

import javax.swing.*;

public class Application {

    private final Arguments args;

    public Application(Arguments args) {
        this.args = args;
    }

    public void start() {
        // --- Настройки сети ---
        String host = args.get(String.class, "--host", "localhost");
        int port = args.get(Integer.class, "--port", 12345);

        // --- Инициализация контекста и зависимостей ---
        EventBus eventBus = new SimpleEventBus();
        ApplicationContext context = new ApplicationContext();
        context.setEventBus(eventBus);
        context.setHost(host);
        context.setPort(port);
        context.setCommandHandler(new CommandHandler(context));

        // --- Ввод имени пользователя ---
        String username = JOptionPane.showInputDialog("Enter your Name:");
        if (username == null || username.isBlank()) username = "Anonymous";
        context.setUsername(username);

        // --- Создание контроллера и UI ---
        ChatClientController controller = new ChatClientController(context);
        UIFacade ui = new SwingUIFacade(controller);

        // --- Связывание ---
        context.setUiFacade(ui);
        controller.setUIFacade(ui);

        // --- Запуск UI ---
        ui.start();

        // --- Подключение к серверу ---
        ChatClientConnector connector = new ChatClientConnector(
                context.getHost(),
                context.getPort(),
                context.getUsername(),
                context.getEventBus()
        );
        connector.connect();
    }

    public static void main(String[] args) {
        // --- Запуск клиента ---
        Arguments parsedArgs = Arguments.parse(args);
        new Application(parsedArgs).start();
    }
}
