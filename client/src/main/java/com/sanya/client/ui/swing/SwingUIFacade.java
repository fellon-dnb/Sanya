package com.sanya.client.ui.swing;

import com.sanya.client.ui.ChatClientController;
import com.sanya.client.ui.UIFacade;
import com.sanya.events.Theme;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class SwingUIFacade implements UIFacade {

    private final ChatClientController controller;
    private final JFrame mainFrame;
    private final JList<String> usersListComponent;
    private final DefaultListModel<String> userListModel;
    private final JTextArea chatArea;  // 🔹 теперь поле, чтобы писать сообщения в чат

    public SwingUIFacade(ChatClientController controller) {
        this.controller = controller;

        // === Инициализация UI ===
        mainFrame = new JFrame("Sanya Chat");
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setSize(700, 500);
        mainFrame.setLocationRelativeTo(null);
        mainFrame.setLayout(new BorderLayout());

        // === Панель списка пользователей ===
        userListModel = new DefaultListModel<>();
        usersListComponent = new JList<>(userListModel);
        JScrollPane scrollPane = new JScrollPane(usersListComponent);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Участники"));

        // === Панель чата ===
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        JScrollPane chatScroll = new JScrollPane(chatArea);
        JTextField inputField = new JTextField();
        JButton sendButton = new JButton("Отправить");

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        JPanel chatPanel = new JPanel(new BorderLayout());
        chatPanel.add(chatScroll, BorderLayout.CENTER);
        chatPanel.add(inputPanel, BorderLayout.SOUTH);

        mainFrame.add(scrollPane, BorderLayout.EAST);
        mainFrame.add(chatPanel, BorderLayout.CENTER);

        // === Логика кнопки ===
        sendButton.addActionListener(e -> {
            String text = inputField.getText().trim();
            if (!text.isEmpty()) {
                controller.onSendMessage(text);
                inputField.setText("");
            }
        });
    }

    @Override
    public void start() {
        SwingUtilities.invokeLater(() -> mainFrame.setVisible(true));
    }

    @Override
    public void showChatWindow() {
        SwingUtilities.invokeLater(() -> {
            mainFrame.setTitle("Sanya — Чат");
            mainFrame.toFront();
        });
    }

    @Override
    public void updateUserList(List<String> users) {
        SwingUtilities.invokeLater(() -> {
            userListModel.clear();
            users.forEach(userListModel::addElement);
        });
    }

    @Override
    public void displayNotification(String message) {
        SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(mainFrame, message, "Уведомление", JOptionPane.INFORMATION_MESSAGE));
    }

    // 🔽 Новые методы

    @Override
    public void appendChatMessage(String text) {
        SwingUtilities.invokeLater(() -> {
            chatArea.append(text + "\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        });
    }

    @Override
    public void setTheme(Theme theme) {
        SwingUtilities.invokeLater(() -> {
            Color bg = theme == Theme.DARK ? new Color(30, 30, 30) : Color.WHITE;
            Color fg = theme == Theme.DARK ? Color.WHITE : Color.BLACK;
            chatArea.setBackground(bg);
            chatArea.setForeground(fg);
            usersListComponent.setBackground(bg);
            usersListComponent.setForeground(fg);
        });
    }

    @Override
    public void showFileTransferProgress(String filename, int percent) {
        SwingUtilities.invokeLater(() -> {
            appendChatMessage("[FILE] " + filename + " — " + percent + "%");
        });
    }
}
