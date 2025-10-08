package com.sanya.client;

import com.ancevt.replines.core.repl.UnknownCommandException;
import com.ancevt.replines.core.repl.integration.LineCallbackOutputStream;
import com.sanya.client.commands.CommandHandler;
import com.sanya.client.ui.FileTransferProgressDialog;
import com.sanya.client.ui.NotificationManager;
import com.sanya.events.*;
import com.sanya.files.FileTransferEvent;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.io.File;

/**
 * Клиентский UI для чата — Swing-окно с EventBus, темами, звуком, уведомлениями и передачей файлов.
 */
public class ChatClientUI extends JFrame {

    private final JTextPane chatPane = new JTextPane();
    private final JTextField inputField = new JTextField();
    private final JButton sendButton = new JButton("Send");
    private final JButton fileButton = new JButton("📁 File");
    private final JToggleButton themeToggle = new JToggleButton();
    private final JToggleButton soundToggle = new JToggleButton();

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

        // 🔹 Верхняя панель (тема и звук)
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
        Font emojiFont;
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            emojiFont = new Font("Segoe UI Emoji", Font.PLAIN, 16);
        } else if (System.getProperty("os.name").toLowerCase().contains("mac")) {
            emojiFont = new Font("Apple Color Emoji", Font.PLAIN, 16);
        } else {
            emojiFont = new Font("Noto Color Emoji", Font.PLAIN, 16);
        }

        themeToggle.setFont(emojiFont);
        soundToggle.setFont(emojiFont);
        themeToggle.setText(ctx.getCurrentTheme() == Theme.DARK ? "\uD83C\uDF1E" : "\uD83C\uDF19");
        soundToggle.setText(ctx.isSoundEnabled() ? "🔊" : "🔈");

        themeToggle.setPreferredSize(new Dimension(60, 60));
        soundToggle.setPreferredSize(new Dimension(60, 60));
        themeToggle.setFocusPainted(false);
        soundToggle.setFocusPainted(false);

        topPanel.add(themeToggle);
        topPanel.add(soundToggle);
        add(topPanel, BorderLayout.NORTH);

        // 🔹 Чат
        chatPane.setEditable(false);
        doc = chatPane.getStyledDocument();
        JScrollPane scrollPane = new JScrollPane(chatPane);

        // 🔹 Стили сообщений
        createStyles(chatPane);

        // 🔹 Панель ввода
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(inputField, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new GridLayout(1, 2));
        buttonPanel.add(fileButton);
        buttonPanel.add(sendButton);
        bottom.add(buttonPanel, BorderLayout.EAST);

        // 🔹 Список пользователей
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.add(new JLabel("Активные пользователи:"), BorderLayout.NORTH);
        rightPanel.add(new JScrollPane(userList), BorderLayout.CENTER);

        add(scrollPane, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
        add(rightPanel, BorderLayout.EAST);

        // 🔹 Цвета по умолчанию для списка
        userList.setBackground(new Color(30, 30, 30));
        userList.setForeground(new Color(230, 230, 230));
        userList.setSelectionBackground(new Color(50, 50, 50));
        userList.setSelectionForeground(Color.WHITE);

        // ==========================
        //  Подписки EventBus
        // ==========================

        eventBus.subscribe(MessageReceivedEvent.class, e -> {
            appendMessage(e.message().toString(), "default");
            if (ctx.isSoundEnabled()) SoundPlayer.playMessageSound();
        });

        eventBus.subscribe(ClearChatEvent.class, e -> {
            clearChat();
            NotificationManager.showInfo("💨 Чат очищен");
        });

        eventBus.subscribe(UserConnectedEvent.class, e -> {
            appendMessage("[SYSTEM] " + e.username() + " entered the chat.\n", "system");
            SwingUtilities.invokeLater(() -> userListModel.addElement(e.username()));
            NotificationManager.showInfo("🟢 " + e.username() + " вошёл в чат");
        });

        eventBus.subscribe(UserDisconnectedEvent.class, e -> {
            appendMessage("[SYSTEM] " + e.username() + " left the chat.\n", "system");
            SwingUtilities.invokeLater(() -> userListModel.removeElement(e.username()));
            NotificationManager.showWarning("🔴 " + e.username() + " покинул чат");
        });

        eventBus.subscribe(UserListUpdatedEvent.class, e -> {
            SwingUtilities.invokeLater(() -> {
                userListModel.clear();
                e.usernames().forEach(userListModel::addElement);
            });
        });

        eventBus.subscribe(ThemeChangedEvent.class, e ->
                SwingUtilities.invokeLater(() -> applyTheme(e.theme())));

        // ==========================
        // 📁 Подписка на FileTransferEvent
        // ==========================
        eventBus.subscribe(FileTransferEvent.class, e -> {
            SwingUtilities.invokeLater(() -> {
                switch (e.type()) {
                    case STARTED -> {
                        appendMessage("[SYSTEM] 📡 Получение файла: " + e.filename(), "system");
                        NotificationManager.showInfo("Получение файла: " + e.filename());
                    }
                    case PROGRESS -> {
                        int percent = (int) ((e.transferredBytes() * 100) / e.totalBytes());
                        if (percent % 10 == 0)
                            appendMessage("[SYSTEM] 🔄 Передача \"" + e.filename() + "\": " + percent + "%", "system");
                    }
                    case COMPLETED -> {
                        appendMessage("[SYSTEM] ✅ Файл получен: " + e.filename(), "system");
                        NotificationManager.showInfo("✅ Файл успешно получен: " + e.filename());
                    }
                    case FAILED -> {
                        appendMessage("[SYSTEM] ❌ Ошибка передачи файла " + e.filename() +
                                ": " + e.errorMessage(), "error");
                        NotificationManager.showError("Ошибка при передаче: " + e.filename());
                    }
                }
            });
        });

        // ==========================
        //  Обработчики кнопок
        // ==========================
        setupSoundToggle();
        setupThemeToggle();
        setupFileButton(eventBus);

        // 🔌 Подключение к серверу
        connector = new ChatClientConnector(host, port, username, eventBus);
        connector.connect();

        // ⚙️ Командный обработчик
        commandHandler = new CommandHandler(eventBus);
        commandHandler.getReplRunner().setOutputStream(
                new LineCallbackOutputStream(line -> {
                    if (line != null && !line.isEmpty()) {
                        SwingUtilities.invokeLater(() ->
                                appendMessage("[SYSTEM] " + line, "system"));
                    }
                })
        );

        // ❌ При закрытии окна
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                connector.close();
            }
        });

        // 🖊️ Отправка сообщений
        sendButton.addActionListener(e -> handleInput());
        inputField.addActionListener(e -> handleInput());

        applyTheme(ctx.getCurrentTheme());
    }

    // ==========================
    //  Методы UI
    // ==========================

    private void setupFileButton(EventBus eventBus) {
        fileButton.addActionListener(e -> handleFileSend(eventBus));
    }

    private void setupSoundToggle() {
        soundToggle.addActionListener(e -> {
            boolean enabled = soundToggle.isSelected();
            ctx.setSoundEnabled(enabled);
            soundToggle.setText(enabled ? "🔊" : "🔈");
        });
    }

    private void setupThemeToggle() {
        themeToggle.addActionListener(e -> {
            Theme newTheme = themeToggle.isSelected() ? Theme.LIGHT : Theme.DARK;
            ctx.getEventBus().publish(new ThemeChangedEvent(newTheme));
            themeToggle.setText(newTheme == Theme.LIGHT ? "🌞" : "🌙");
        });
    }

    private void handleFileSend(EventBus eventBus) {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            new Thread(() -> {
                FileTransferProgressDialog dialog =
                        new FileTransferProgressDialog(this, file.getName());
                dialog.setVisible(true);
                try {
                    FileSender.sendFile(file, ctx.getUsername(),
                            connector.getOutputStream(), eventBus);
                } catch (Exception ex) {
                    NotificationManager.showError("Ошибка при отправке файла: " + ex.getMessage());
                } finally {
                    dialog.dispose();
                }
            }).start();
        }
    }

    private void createStyles(JTextPane pane) {
        Style def = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);

        Style regular = pane.addStyle("default", def);
        StyleConstants.setFontFamily(regular, "Consolas");
        StyleConstants.setFontSize(regular, 14);
        StyleConstants.setForeground(regular, Color.BLACK);

        Style system = pane.addStyle("system", def);
        StyleConstants.setItalic(system, true);
        StyleConstants.setForeground(system, new Color(180, 180, 180));

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

    private void applyTheme(Theme theme) {
        Color bg, fg, system, error;

        if (theme == Theme.LIGHT) {
            bg = new Color(245, 245, 245);
            fg = Color.BLACK;
            system = new Color(80, 80, 80);
            error = new Color(200, 30, 30);
        } else {
            bg = new Color(25, 25, 25);
            fg = new Color(230, 230, 230);
            system = new Color(180, 180, 180);
            error = new Color(255, 100, 100);
        }

        chatPane.setBackground(bg);
        userList.setBackground(theme == Theme.LIGHT ? new Color(240, 240, 240) : new Color(30, 30, 30));
        userList.setForeground(fg);
        inputField.setBackground(bg.darker());
        inputField.setForeground(fg);
        sendButton.setBackground(bg);
        sendButton.setForeground(fg);

        // Обновляем цвета стилей
        Style regular = chatPane.getStyle("default");
        StyleConstants.setForeground(regular, fg);

        Style systemStyle = chatPane.getStyle("system");
        StyleConstants.setForeground(systemStyle, system);

        Style errorStyle = chatPane.getStyle("error");
        StyleConstants.setForeground(errorStyle, error);

        // 🔥 Новое: перекрашиваем уже выведенный текст
        StyledDocument doc = chatPane.getStyledDocument();
        SwingUtilities.invokeLater(() -> {
            try {
                // Перекрашиваем весь текст (более корректно, чем просто repaint)
                String text = doc.getText(0, doc.getLength());
                doc.remove(0, doc.getLength());
                doc.insertString(0, text, regular);
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        });

        chatPane.repaint();
    }
}
