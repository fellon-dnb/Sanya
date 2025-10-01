package com.sanya;

import com.ancevt.replines.core.argument.Arguments;

import javax.swing.*;
import java.awt.*;

public class ChatClientUI extends JFrame {

    private final JTextArea chatArea = new JTextArea();
    private final JTextField inputField = new JTextField();
    private final JButton sendButton = new JButton("Send");

    private final ChatClientConnector connector;

    public ChatClientUI(String host, int port, String name) {
        setTitle("Chat Client - " + name);
        setSize(400, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        chatArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(chatArea);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(inputField, BorderLayout.CENTER);
        bottom.add(sendButton, BorderLayout.EAST);

        add(scrollPane, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        connector = new ChatClientConnector(host, port, name, new ChatUiCallback() {
            @Override
            public void onMessage(Message message) {
                appendMessage(message.toString());
            }

            @Override
            public void onError(Throwable t) {
                appendMessage("ERROR: " + t.getMessage());
            }
        });

        connector.connect();

        sendButton.addActionListener(e -> sendCurrent());
        inputField.addActionListener(e -> sendCurrent());
    }

    private void sendCurrent() {
        String text = inputField.getText().trim();
        if (!text.isEmpty()) {
            connector.sendMessage(text);
            inputField.setText("");
        }
    }

    private void appendMessage(String msg) {
        chatArea.append(msg + "\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }

    public static void main(String[] args) {
        Arguments a = Arguments.parse(args);

        String host = a.get(String.class, "--host", "localhost");
        int port = a.get(int.class, "--port", 12345);

        SwingUtilities.invokeLater(() -> {
            String name = JOptionPane.showInputDialog("Введите имя:");
            ChatClientUI ui = new ChatClientUI(host, port, name);
            ui.setVisible(true);
        });
    }
}
