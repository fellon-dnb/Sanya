package com.sanya;

import com.ancevt.replines.core.argument.Arguments;
import com.ancevt.replines.core.repl.UnknownCommandException;
import com.ancevt.replines.core.repl.integration.LineCallbackOutputStream;
import com.ancevt.replines.core.repl.integration.PushableInputStream;
import com.ancevt.replines.core.repl.integration.ReplSwingConnector;
import com.sanya.commands.CommandHandler;
import com.sanya.events.*;

import javax.swing.*;
import java.awt.*;

/**
 * Клиентский UI для чата — Swing-окно с EventBus и поддержкой команд.
 */
public class ChatClientUI extends JFrame {

    private final JTextArea chatArea = new JTextArea();
    private final JTextField inputField = new JTextField();
    private final JButton sendButton = new JButton("Send");

    private final EventBus eventBus = new SimpleEventBus();
    private final ChatClientConnector connector;
    private final CommandHandler commandHandler;

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

        // Подписки EventBus
        eventBus.subscribe(MessageReceivedEvent.class, e -> appendMessage(e.message().toString()));
        eventBus.subscribe(ClearChatEvent.class, e -> clearChat());

        // Подключаемся к серверу
        connector = new ChatClientConnector(host, port, name, eventBus);
        connector.connect();

        // Командный обработчик (поддержка /help, /exit, /clear)
        commandHandler = new CommandHandler(eventBus);



        // При закрытии окна
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                connector.close();
            }
        });

        LineCallbackOutputStream outputStream = new LineCallbackOutputStream(line -> {
            if (line != null && !line.isEmpty()) {
                SwingUtilities.invokeLater(() -> {
                    chatArea.append(line + "\n");
                    chatArea.setCaretPosition(chatArea.getDocument().getLength());
                });
            }
        });

        commandHandler.getReplRunner().setOutputStream(outputStream);

//        ReplSwingConnector.launch(inputField, chatArea, commandHandler.getReplRunner(), true);

        // Обработка кнопки и Enter
        sendButton.addActionListener(e -> handleInput());
        inputField.addActionListener(e -> handleInput());
    }

    /**
     * Обрабатывает ввод пользователя — сообщение или команду.
     */
    private void handleInput() {
        String text = inputField.getText().trim();
        if (text.isEmpty()) return;

        if (text.startsWith("/")) {
            try {
                commandHandler.getReplRunner().execute(text);
            } catch (UnknownCommandException e) {
                throw new RuntimeException(e);
            }
        } else {
            eventBus.publish(new MessageSendEvent(text));
        }

        inputField.setText("");
    }

    private void appendMessage(String msg) {
        chatArea.append(msg + "\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }

    private void clearChat() {
        chatArea.setText("");
    }

    public static void main(String[] args) {
        Arguments a = Arguments.parse(args);
        String host = a.get(String.class, "--host", "localhost");
        int port = a.get(Integer.class, "--port", 12345);

        SwingUtilities.invokeLater(() -> {
            String name = JOptionPane.showInputDialog("Enter your Name:");
            if (name == null || name.isBlank()) name = "Anonymous";
            ChatClientUI ui = new ChatClientUI(host, port, name);
            ui.setVisible(true);
        });
    }
}
