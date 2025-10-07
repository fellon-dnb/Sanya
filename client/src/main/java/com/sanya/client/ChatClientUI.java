package com.sanya.client;

import com.ancevt.replines.core.repl.UnknownCommandException;
import com.ancevt.replines.core.repl.integration.LineCallbackOutputStream;
import com.sanya.client.commands.CommandHandler;
import com.sanya.events.*;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;

/**
 * Клиентский UI для чата — Swing-окно с EventBus и поддержкой стилей сообщений.
 */
public class ChatClientUI extends JFrame {

    private final JTextPane chatPane = new JTextPane();
    private final JTextField inputField = new JTextField();
    private final JButton sendButton = new JButton("Send");

    private final ChatClientConnector connector;
    private final CommandHandler commandHandler;
    private final ApplicationContext ctx;
    private final DefaultListModel<String> userListModel = new DefaultListModel<>();
    private final JList<String> userList = new JList<>(userListModel);
    private final StyledDocument doc;

    public ChatClientUI(ApplicationContext ctx) {
        this.ctx = ctx;
        String username = ctx.getUsername();
        String host = ctx.getHost();
        int port = ctx.getPort();
        EventBus eventBus = ctx.getEventBus();

        setTitle("Chat Client - " + username);
        setSize(700, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        chatPane.setEditable(false);
        doc = chatPane.getStyledDocument();
        JScrollPane scrollPane = new JScrollPane(chatPane);

        // Создаём стили
        createStyles(chatPane);

        // Панель чата и ввода
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(inputField, BorderLayout.CENTER);
        bottom.add(sendButton, BorderLayout.EAST);

        // Список пользователей
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.add(new JLabel("Активные пользователи:"), BorderLayout.NORTH);
        rightPanel.add(new JScrollPane(userList), BorderLayout.CENTER);

        add(scrollPane, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
        add(rightPanel, BorderLayout.EAST);
        //DarkTheme of List
        userList.setBackground(new Color(30, 30, 30));
        userList.setForeground(new Color(230, 230, 230));
        userList.setSelectionBackground(new Color(50, 50, 50));
        userList.setSelectionForeground(new Color(255, 255, 255));
        // Подписки EventBus
        eventBus.subscribe(MessageReceivedEvent.class, e -> appendMessage(e.message().toString(), "default"));
        eventBus.subscribe(ClearChatEvent.class, e -> clearChat());

        eventBus.subscribe(UserConnectedEvent.class, e -> {
            appendMessage("[SYSTEM] " + e.username() + " entered the chat.\n", "system");
            SwingUtilities.invokeLater(() -> userListModel.addElement(e.username()));
        });

        eventBus.subscribe(UserDisconnectedEvent.class, e -> {
            appendMessage("[SYSTEM] " + e.username() + " left the chat.\n", "system");
            SwingUtilities.invokeLater(() -> userListModel.removeElement(e.username()));
        });

        eventBus.subscribe(UserListUpdatedEvent.class, e -> {
            SwingUtilities.invokeLater(() -> {
                userListModel.clear();
                e.usernames().forEach(userListModel::addElement);
            });
        });

        // Подключаемся к серверу
        connector = new ChatClientConnector(host, port, username, eventBus);
        connector.connect();

        // Командный обработчик
        commandHandler = new CommandHandler(eventBus);
        commandHandler.getReplRunner().setOutputStream(
                new com.ancevt.replines.core.repl.integration.LineCallbackOutputStream(line -> {
                    if (line != null && !line.isEmpty()) {
                        SwingUtilities.invokeLater(() ->
                                appendMessage("[SYSTEM] " + line, "system"));
                    }
                })
        );

        // При закрытии окна
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                connector.close();
            }
        });

        // Обработка кнопки и Enter
        sendButton.addActionListener(e -> handleInput());
        inputField.addActionListener(e -> handleInput());
    }

    /** Создаёт стили для форматирования сообщений. */
    private void createStyles(JTextPane pane) {
        Style def = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);
        //User стайл
        Style regular = pane.addStyle("default", def);
        StyleConstants.setFontFamily(regular, "Consolas");
        StyleConstants.setFontSize(regular, 14);
        StyleConstants.setForeground(regular, Color.BLACK);
       // Системный стайл
        Style system = pane.addStyle("system", def);
        StyleConstants.setItalic(system, true);
        StyleConstants.setForeground(system, new Color(180, 180, 180));
        //ошибковый стайл
        Style error = pane.addStyle("error", def);
        StyleConstants.setForeground(error, Color.RED);
        StyleConstants.setBold(error, true);
    }

    private void handleInput() {
        String text = inputField.getText().trim();
        if (text.isEmpty()) return;

        if (text.startsWith("/")) {
            try {
                commandHandler.getReplRunner().execute(text);
            } catch (UnknownCommandException e) {
                appendMessage("[SYSTEM] Unknown command: " + text.split(" ")[0] + "\n", "error");
            }
        } else {
            ctx.getEventBus().publish(new MessageSendEvent(text));
        }

        inputField.setText("");
    }

    private void appendMessage(String msg, String style) {
        SwingUtilities.invokeLater(() -> {
            try {
                doc.insertString(doc.getLength(), msg + "\n", doc.getStyle(style));
                chatPane.setCaretPosition(doc.getLength());
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        });
    }

    private void clearChat() {
        SwingUtilities.invokeLater(() -> chatPane.setText(""));
    }
}
