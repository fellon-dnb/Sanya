package com.sanya.client;

import com.ancevt.replines.core.argument.Arguments;

import javax.swing.*;

public class Application {

    public void start(Arguments args) {
        ConnectionInfo connectionInfo = new ConnectionInfo(
                args.get(String.class, "--host", "localhost"),
                args.get(Integer.class, "--port", 12345)
        );

        ApplicationContext ctx = new ApplicationContext(connectionInfo);

        SwingUtilities.invokeLater(() -> {
            String username = JOptionPane.showInputDialog("Enter your Name:");
            if (username == null || username.isBlank()) username = "Anonymous";

            ctx.getUserInfo().setName(username);

            ChatClientUI ui = new ChatClientUI(ctx);
            ui.setVisible(true);
        });
    }

    public static void main(String[] args) {
        new Application().start(Arguments.parse(args));
    }
}
