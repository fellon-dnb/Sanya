package com.sanya;

import com.sanya.events.*;
import com.ancevt.replines.core.argument.Arguments;
import javax.swing.*;
import java.awt.*;

public class ChatClientUI extends JFrame {

    private final JTextArea chatArea = new JTextArea();
    private final JTextField inputField = new JTextField();
    private final JButton sendButton = new JButton("Send");

    private final EventBus eventBus = new SimpleEventBus();
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

        // Подписка: получаем входящие сообщения и печатаем
        eventBus.subscribe(MessageReceivedEvent.class, e -> {
            appendMessage(e.message().toString());
        });

        connector = new ChatClientConnector(host, port, name, eventBus);
        connector.connect();

        // При нажатии публикуем событие MessageSendEvent
        sendButton.addActionListener(e -> sendCurrent());
        inputField.addActionListener(e -> sendCurrent());

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent e) {
                connector.close();
            }
        });
    }

    private void sendCurrent() {
        String text = inputField.getText().trim();
        if (!text.isEmpty()) {
            eventBus.publish(new MessageSendEvent(text));
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
        int port = a.get(Integer.class, "--port", 12345);

        SwingUtilities.invokeLater(() -> {
            String name = JOptionPane.showInputDialog("Enter your Name:");
            ChatClientUI ui = new ChatClientUI(host, port, name);
            ui.setVisible(true);
        });
    }
}
