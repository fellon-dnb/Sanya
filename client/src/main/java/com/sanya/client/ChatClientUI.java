package com.sanya.client;

import com.ancevt.replines.core.repl.UnknownCommandException;
import com.ancevt.replines.core.repl.io.BufferedLineOutputStream;
import com.sanya.client.audio.VoicePlayer;
import com.sanya.client.audio.VoicePlayer2;
import com.sanya.client.audio.VoiceRecorder;
import com.sanya.client.files.FileSender;
import com.sanya.client.ui.FileTransferProgressDialog;
import com.sanya.client.ui.NotificationManager;
import com.sanya.events.*;
import com.sanya.files.FileChunk;
import com.sanya.files.FileTransferEvent;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.ObjectOutputStream;

import static com.sanya.client.audio.VoicePlayer2.*;

/**
 * ChatClientUI — графический интерфейс клиента.
 * Поддерживает чат, файлы и голосовые сообщения.
 */
public class ChatClientUI extends JFrame {

    private final JTextPane chatPane = new JTextPane();
    private final JTextField inputField = new JTextField();
    private final JButton sendButton = new JButton("Send");
    private final JButton fileButton = new JButton("📎 File");
    private final JButton voiceButton = new JButton("🎤 Voice");
    private final JToggleButton themeToggle = new JToggleButton();
    private final JToggleButton soundToggle = new JToggleButton();
    private final DefaultListModel<String> userListModel = new DefaultListModel<>();
    private final JList<String> userList = new JList<>(userListModel);

    private final ApplicationContext ctx;
    private final EventBus eventBus;
    private final ChatClientConnector connector;
    private final StyledDocument doc;

    private VoiceRecorder recorder;
    private boolean recording = false;

    public ChatClientUI(ApplicationContext ctx) {
        this.ctx = ctx;
        this.eventBus = ctx.getEventBus();

        setTitle("Chat - " + ctx.getUsername());
        setSize(800, 550);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Верхняя панель
        JPanel top = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        themeToggle.setText(ctx.getCurrentTheme() == Theme.DARK ? "☀️" : "🌙");
        soundToggle.setText(ctx.isSoundEnabled() ? "🔊" : "🔇");
        top.add(themeToggle);
        top.add(soundToggle);
        add(top, BorderLayout.NORTH);

        // Центр — чат
        chatPane.setEditable(false);
        doc = chatPane.getStyledDocument();
        createStyles(chatPane);
        JScrollPane chatScroll = new JScrollPane(chatPane);
        add(chatScroll, BorderLayout.CENTER);

        // Правая панель — пользователи
        JPanel right = new JPanel(new BorderLayout());
        right.add(new JLabel("Active users:"), BorderLayout.NORTH);
        right.add(new JScrollPane(userList), BorderLayout.CENTER);
        add(right, BorderLayout.EAST);

        // Нижняя панель — ввод
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(inputField, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new GridLayout(1, 3));
        buttons.add(fileButton);
        buttons.add(sendButton);
        buttons.add(voiceButton);
        bottom.add(buttons, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);

        // Подключение
        connector = new ChatClientConnector(ctx.getHost(), ctx.getPort(), ctx.getUsername(), eventBus);
        connector.connect();

        // Подписки
        subscribeEvents();

        // Обработчики
        sendButton.addActionListener(e -> handleInput());
        inputField.addActionListener(e -> handleInput());
        fileButton.addActionListener(e -> handleFileSend());
        voiceButton.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { startRecording(); }
            @Override public void mouseReleased(MouseEvent e) { stopRecording(); }
        });
        themeToggle.addActionListener(e -> toggleTheme());
        soundToggle.addActionListener(e -> toggleSound());

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                connector.close();
            }
        });

        applyTheme(ctx.getCurrentTheme());
    }

    private void subscribeEvents() {
        // 1️⃣ входящие текстовые сообщения
        eventBus.subscribe(MessageReceivedEvent.class, e -> {
            appendMessage(e.message().toString(), "default");
            if (ctx.isSoundEnabled()) SoundPlayer.playMessageSound();
        });

        // 2️⃣ обновление списка пользователей
        eventBus.subscribe(UserListUpdatedEvent.class, e ->
                SwingUtilities.invokeLater(() -> {
                    userListModel.clear();
                    e.usernames().forEach(userListModel::addElement);
                })
        );

        // 3️⃣ пользователь вошёл
        eventBus.subscribe(UserConnectedEvent.class, e ->
                appendMessage("[SYSTEM] " + e.username() + " joined", "system"));

        // 4️⃣ пользователь вышел
        eventBus.subscribe(UserDisconnectedEvent.class, e ->
                appendMessage("[SYSTEM] " + e.username() + " left", "system"));

        // 5️⃣ очистка чата
        eventBus.subscribe(ClearChatEvent.class, e ->
                SwingUtilities.invokeLater(() -> chatPane.setText("")));

        // 6️⃣ кто-то начал или закончил запись голосового
        eventBus.subscribe(VoiceRecordingEvent.class, e -> {
            if (!e.username().equals(ctx.getUsername())) {
                String text = e.started()
                        ? "[SYSTEM] " + e.username() + " записывает голосовое сообщение..."
                        : "[SYSTEM] " + e.username() + " закончил запись";
                appendMessage(text, "system");
            }
        });

        // 7️⃣ получено готовое голосовое сообщение
        eventBus.subscribe(VoiceMessageReadyEvent.class, evt -> {
            SwingUtilities.invokeLater(() -> {
                JButton playButton = new JButton("▶ Прослушать");
                playButton.addActionListener(e -> {
                    new Thread(() -> {
                        new VoicePlayer(evt.data()).play();
                    }, "VoicePlayer").start();
                });

                appendMessage("[🎤 Голосовое сообщение от " + evt.username() + "]", "info");
                chatPane.insertComponent(playButton);
                appendMessage("", "info");
            });
        });

        // 8️⃣ кто-то нажал "прослушать"
        eventBus.subscribe(VoicePlayEvent.class, e -> {
            if (!e.username().equals(ctx.getUsername())) {
                appendMessage("[SYSTEM] " + e.username() + " прослушал голосовое сообщение", "system");
            }
        });

        // 9️⃣ входящие голосовые фрагменты старого формата (FileChunk "voice")
        eventBus.subscribe(VoiceReceivedEvent.class, e -> play(e.data()));

        eventBus.subscribe(VoiceRecordingStoppedEvent.class, e -> {
            SwingUtilities.invokeLater(() -> {
                int opt = JOptionPane.showConfirmDialog(
                        this,
                        "Отправить голосовое сообщение?",
                        "Голосовое сообщение",
                        JOptionPane.YES_NO_OPTION);
                if (opt == JOptionPane.YES_OPTION) {
                    try {
                        ObjectOutputStream out = connector.getOutputStream();
                        out.writeObject(new VoiceMessageReadyEvent(e.username(), e.data()));
                        out.flush();
                    } catch (Exception ex) {
                        appendMessage("[SYSTEM] Ошибка отправки голосового: " + ex.getMessage(), "error");
                    }
                }
            });
        });

    }


    private void handleInput() {
        String text = inputField.getText().trim();
        if (text.isEmpty()) return;

        if (text.startsWith("/")) {
            try {
                ctx.getCommandHandler().getReplRunner().execute(text);
            } catch (UnknownCommandException ex) {
                appendMessage("[SYSTEM] Unknown command", "error");
            }
        } else {
            eventBus.publish(new MessageSendEvent(text));
        }
        inputField.setText("");
    }

    private void handleFileSend() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            new Thread(() -> {
                FileTransferProgressDialog.open(this, file.getName(), true);
                try {
                    FileSender.sendFile(file, ctx.getUsername(), connector.getOutputStream(), eventBus);
                } catch (Exception ex) {
                    NotificationManager.showError("File send failed: " + ex.getMessage());
                }
            }).start();
        }
    }

    private void toggleRecording() {
        if (!recording) {
            recorder = new VoiceRecorder(ctx);
            new Thread(recorder, "VoiceRecorder").start();
            recording = true;
        } else {
            recorder.stop();
            recording = false;
        }
    }
    private void appendComponent(JComponent c) {
        SwingUtilities.invokeLater(() -> {
            chatPane.setCaretPosition(doc.getLength());
            chatPane.insertComponent(c);
            try { doc.insertString(doc.getLength(), "\n", null); } catch (BadLocationException ignored) {}
        });
    }
    private void toggleTheme() {
        Theme newTheme = themeToggle.isSelected() ? Theme.LIGHT : Theme.DARK;
        ctx.setCurrentTheme(newTheme);
        themeToggle.setText(newTheme == Theme.DARK ? "☀️" : "🌙");
        applyTheme(newTheme);
    }

    private void toggleSound() {
        boolean enabled = soundToggle.isSelected();
        ctx.setSoundEnabled(enabled);
        soundToggle.setText(enabled ? "🔊" : "🔇");
    }

    private void createStyles(JTextPane pane) {
        Style def = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);
        Style regular = pane.addStyle("default", def);
        StyleConstants.setFontFamily(regular, "Consolas");
        StyleConstants.setFontSize(regular, 14);
        StyleConstants.setForeground(regular, Color.WHITE);

        Style system = pane.addStyle("system", def);
        StyleConstants.setItalic(system, true);
        StyleConstants.setForeground(system, new Color(180, 180, 180));

        Style error = pane.addStyle("error", def);
        StyleConstants.setBold(error, true);
        StyleConstants.setForeground(error, Color.RED);
    }

    private void appendMessage(String msg, String style) {
        SwingUtilities.invokeLater(() -> {
            try {
                doc.insertString(doc.getLength(), msg + "\n", doc.getStyle(style));
                chatPane.setCaretPosition(doc.getLength());
            } catch (BadLocationException ignored) {}
        });
    }
    private void startRecording() {
        if (recording) return;
        recording = true;
        recorder = new VoiceRecorder(ctx);
        new Thread(recorder, "VoiceRecorder").start();
        eventBus.publish(new VoiceRecordingEvent(ctx.getUsername(), true));
    }

    private void stopRecording() {
        if (!recording) return;
        recording = false;
        if (recorder != null) recorder.stop();
        eventBus.publish(new VoiceRecordingEvent(ctx.getUsername(), false));
    }

    private void applyTheme(Theme theme) {
        if (theme == Theme.DARK) {
            chatPane.setBackground(new Color(25, 25, 25));
            chatPane.setForeground(Color.WHITE);
            inputField.setBackground(new Color(40, 40, 40));
            inputField.setForeground(Color.WHITE);
            userList.setBackground(new Color(30, 30, 30));
            userList.setForeground(Color.WHITE);
        } else {
            chatPane.setBackground(Color.WHITE);
            chatPane.setForeground(Color.BLACK);
            inputField.setBackground(Color.LIGHT_GRAY);
            inputField.setForeground(Color.BLACK);
            userList.setBackground(Color.WHITE);
            userList.setForeground(Color.BLACK);
        }
    }
}
