package com.sanya.client;

import com.ancevt.replines.core.argument.Arguments;
import com.sanya.events.EventBus;
import com.sanya.events.SimpleEventBus;

import javax.naming.Context;
import javax.swing.*;

public class Application {

    private final Arguments args;

    private ChatClientUI ui;

    private Context ctx;

    public Application(Arguments args) {
        this.args = args;
    }

    public void start() {
        String host = args.get(String.class, "--host", "localhost");
        int port = args.get(Integer.class, "--port", 12345);

        EventBus eventBus = new SimpleEventBus();

        ApplicationContext ctx = new ApplicationContext();
        ctx.setHost(host);
        ctx.setPort(port);
        ctx.setContext(ctx);
        ctx.setEventBus(eventBus);

        SwingUtilities.invokeLater(() -> {
            String username = JOptionPane.showInputDialog("Enter your Name:");
            if (username == null || username.isBlank()) username = "Anonymous";

            ctx.setUsername(username);

            ChatClientUI ui = new ChatClientUI(ctx);
            ui.setVisible(true);
        });
    }

    public static void main(String[] args) {
        new Application(Arguments.parse(args)).start();
    }
}
