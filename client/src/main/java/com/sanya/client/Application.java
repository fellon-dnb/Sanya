package com.sanya.client;

import com.ancevt.replines.core.argument.Arguments;
import com.sanya.client.commands.CommandHandler;
import com.sanya.events.EventBus;
import com.sanya.events.SimpleEventBus;

import javax.swing.*;

public class Application {

    public void start(Arguments args) {
        String host = args.get(String.class, "--host", "localhost");
        int port = args.get(Integer.class, "--port", 12345);

        EventBus eventBus = new SimpleEventBus();

        ApplicationContext ctx = new ApplicationContext();
        ctx.setHost(host);
        ctx.setPort(port);
        ctx.setEventBus(eventBus);
        ctx.setCommandHandler(new CommandHandler(ctx));

        SwingUtilities.invokeLater(() -> {
            String username = JOptionPane.showInputDialog("Enter your Name:");
            if (username == null || username.isBlank()) username = "Anonymous";

            ctx.setUsername(username);

            ChatClientUI ui = new ChatClientUI(ctx);
            ui.setVisible(true);
        });
    }

    public static void main(String[] args) {
        new Application().start(Arguments.parse(args));
    }
}
