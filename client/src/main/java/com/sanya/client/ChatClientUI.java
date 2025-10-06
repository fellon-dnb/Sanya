package com.sanya.client;

import com.ancevt.replines.core.repl.UnknownCommandException;
import com.ancevt.replines.core.repl.integration.LineCallbackOutputStream;
import com.sanya.client.commands.CommandHandler;
import com.sanya.events.ClearChatEvent;
import com.sanya.events.EventBus;
import com.sanya.events.MessageReceivedEvent;
import com.sanya.events.MessageSendEvent;

import javax.swing.*;
import java.awt.*;

/**
 * Клиентский UI для чата — Swing-окно с EventBus и поддержкой команд.
 */
public class ChatClientUI extends JFrame {

    private final JTextArea chatArea = new JTextArea();
    private final JTextField inputField = new JTextField();
    private final JButton sendButton = new JButton("Send");

    private final ChatClientConnector connector;
    private final CommandHandler commandHandler;
    private final ApplicationContext ctx;
    private final LineCallbackOutputStream outputStream;

    public ChatClientUI(ApplicationContext ctx) {
        this.ctx = ctx;
        String username = ctx.getUsername();
        String host = ctx.getHost();
        int port = ctx.getPort();

        EventBus eventBus = ctx.getEventBus();

        setTitle("Chat Client - " + username);
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
        connector = new ChatClientConnector(host, port, username, eventBus);
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

        outputStream = new LineCallbackOutputStream(line -> {
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
                chatArea.append("Unknown command: " + text.split(" ")[0]);
                e.printStackTrace();
            }
        } else {
            ctx.getEventBus().publish(new MessageSendEvent(text));
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

}
