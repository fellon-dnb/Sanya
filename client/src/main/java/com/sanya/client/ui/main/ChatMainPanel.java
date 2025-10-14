package com.sanya.client.ui.main;

import com.sanya.client.ApplicationContext;
import com.sanya.client.ui.input.ChatInputPanel;
import com.sanya.events.Theme;
import java.util.List;
import javax.swing.*;
import java.awt.*;

public class ChatMainPanel extends JFrame {
    private final JTextPane chatPane = new JTextPane();
    private final DefaultListModel<String> userListModel = new DefaultListModel<>();
    private final JList<String> userList = new JList<>(userListModel);
    private final ChatInputPanel inputPanel;

    public ChatMainPanel(ApplicationContext ctx) {
        setTitle("Chat - " + ctx.getUserSettings().getName());
        setLayout(new BorderLayout());
        add(new JScrollPane(chatPane), BorderLayout.CENTER);
        add(new JScrollPane(userList), BorderLayout.EAST);

        inputPanel = new ChatInputPanel(ctx);
        add(inputPanel, BorderLayout.SOUTH);
        setSize(800, 550);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
    }

    public void appendChatMessage(String msg) { /* ... */ }

    public void appendSystemMessage(String msg) { /* ... */ }

    public void clearChat() { /* ... */ }

    public void updateUserList(List<String> users) { /* ... */ }

    public void applyTheme(Theme theme) { /* ... */ }
}