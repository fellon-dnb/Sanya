package com.sanya.client;

import com.ancevt.replines.core.argument.Arguments;
import com.sanya.client.settings.NetworkSettings;

import javax.swing.*;

public class Application {

    public void start(Arguments args) {
        NetworkSettings networkSettings = new NetworkSettings(
                args.get(String.class, new String[]{"--host", "-h"}, "localhost"),
                args.get(Integer.class, new String[]{"--port", "-p"}, 12345)
        );

        String usernameCli = args.get(String.class, new String[]{"--username", "-u"}, "");

        ApplicationContext ctx = new ApplicationContext(networkSettings);

        SwingUtilities.invokeLater(() -> {
            if (usernameCli.isEmpty()) {
                String username = JOptionPane.showInputDialog("Enter your Name:");
                if (username == null || username.isBlank()) username = "Anonymous";
                ctx.getUserSettings().setName(username);
            } else {
                ctx.getUserSettings().setName(usernameCli);
            }

            ChatClientUI ui = new ChatClientUI(ctx);
            ui.setVisible(true);
        });
    }

    public static void main(String[] args) {
        new Application().start(Arguments.parse(args));
    }
}
