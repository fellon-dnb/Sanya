package com.sanya.client;

import com.ancevt.replines.core.repl.UnknownCommandException;
import com.sanya.client.audio.VoicePlayer;
import com.sanya.client.audio.VoiceRecorder;
import com.sanya.client.files.FileSender;
import com.sanya.client.ui.FileTransferProgressDialog;
import com.sanya.client.ui.NotificationManager;
import com.sanya.events.*;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import javax.swing.Timer;


/**
 * Полноценный клиент чата с поддержкой тем, файлов и голосовых сообщений.
 */
public class ChatClientUI extends JFrame {

    private static final int USERS_WIDTH = 180;

    private final JTextPane chatPane = new JTextPane();
    private final JTextField inputField = new JTextField();
    private final JButton sendButton = new JButton("Send");
    private final JButton fileButton = new JButton("📁 File");
    private final JButton voiceButton = new JButton("🎤 Voice");
    private final JToggleButton themeToggle = new JToggleButton();
    private final JToggleButton soundToggle = new JToggleButton();
    private final DefaultListModel<String> userListModel = new DefaultListModel<>();
    private final JList<String> userList = new JList<>(userListModel);
    private final JLabel recordStatusLabel = new JLabel(" ");
    private Timer recordTimer;
    private long recordStartTime = 0;

    private final ApplicationContext ctx;
    private final EventBus eventBus;
    private final ChatClientConnector connector;
    private final StyledDocument doc;

    private VoiceRecorder recorder;
    private boolean recording = false;

    public ChatClientUI(ApplicationContext ctx) {
        this.ctx = ctx;
        this.eventBus = ctx.getEventBus();

        setTitle("Chat Client - " + ctx.getUserInfo().getName());
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(800, 550);
        setMinimumSize(new Dimension(800, 550));
        setLayout(new BorderLayout());
        setLocationRelativeTo(null);

        // Верхняя панель
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
        themeToggle.setText(ctx.getTheme() == Theme.DARK ? "☀️" : "🌙");
        soundToggle.setText(ctx.isSoundEnabled() ? "🔊" : "🔈");
        topPanel.add(themeToggle);
        topPanel.add(soundToggle);
        add(topPanel, BorderLayout.NORTH);

        // Центр — чат
        chatPane.setEditable(false);
        chatPane.setMargin(new Insets(6, 6, 6, 6));
        doc = chatPane.getStyledDocument();
        createStyles(chatPane);
        JScrollPane chatScroll = new JScrollPane(chatPane);
        chatScroll.setBorder(BorderFactory.createEmptyBorder());
        add(chatScroll, BorderLayout.CENTER);

        // Правая панель — пользователи
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.add(new JLabel("Active users:"), BorderLayout.NORTH);
        JScrollPane userScroll = new JScrollPane(userList);
        Dimension uw = new Dimension(USERS_WIDTH, 0);
        userScroll.setPreferredSize(uw);
        userScroll.setMinimumSize(uw);
        userScroll.setMaximumSize(new Dimension(USERS_WIDTH, Integer.MAX_VALUE));
        rightPanel.setPreferredSize(uw);
        rightPanel.add(userScroll, BorderLayout.CENTER);
        add(rightPanel, BorderLayout.EAST);

        // Нижняя панель — ввод
        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        bottomPanel.add(inputField, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new GridLayout(1, 3, 5, 0));
        buttonPanel.add(fileButton);
        buttonPanel.add(sendButton);
        buttonPanel.add(voiceButton);
        bottomPanel.add(buttonPanel, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);
        bottomPanel.add(recordStatusLabel, BorderLayout.WEST);
        // Подключение
        connector = new ChatClientConnector(
                ctx.getConnectionInfo().getHost(),
                ctx.getConnectionInfo().getPort(),
                ctx.getUserInfo().getName(),
                eventBus
        );
        connector.connect();

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

        applyTheme(ctx.getTheme());
    }

    // ========================== События ==========================
    private void subscribeEvents() {
        eventBus.subscribe(MessageReceivedEvent.class, e -> {
            appendMessage(e.message().toString(), "default");
            if (ctx.isSoundEnabled()) SoundPlayer.playMessageSound();
        });

        eventBus.subscribe(UserConnectedEvent.class, e ->
                appendMessage("[SYSTEM] " + e.username() + " joined", "system"));

        eventBus.subscribe(UserDisconnectedEvent.class, e ->
                appendMessage("[SYSTEM] " + e.username() + " left", "system"));

        eventBus.subscribe(UserListUpdatedEvent.class, e ->
                SwingUtilities.invokeLater(() -> {
                    userListModel.clear();
                    e.usernames().forEach(userListModel::addElement);
                }));

        eventBus.subscribe(ClearChatEvent.class, e ->
                SwingUtilities.invokeLater(() -> chatPane.setText("")));

        eventBus.subscribe(VoiceRecordingEvent.class, e -> {
            if (!e.username().equals(ctx.getUserInfo().getName())) {
                String text = e.started()
                        ? "[SYSTEM] " + e.username() + " записывает голосовое..."
                        : "[SYSTEM] " + e.username() + " закончил запись.";
                appendMessage(text, "system");
            }
        });

        // Отправка готового голосового после остановки записи
        eventBus.subscribe(VoiceRecordingStoppedEvent.class, e -> {
            SwingUtilities.invokeLater(() -> {
                int opt = JOptionPane.showConfirmDialog(
                        this,
                        "Отправить голосовое сообщение?",
                        "Голосовое",
                        JOptionPane.YES_NO_OPTION
                );
                if (opt == JOptionPane.YES_OPTION) {
                    try {
                        connector.getOutputStream().writeObject(
                                new VoiceMessageReadyEvent(e.username(), e.data())
                        );
                        connector.getOutputStream().flush();
                    } catch (Exception ex) {
                        appendMessage("[SYSTEM] Ошибка отправки голосового: " + ex.getMessage(), "error");
                    }
                } else {
                    appendMessage("[SYSTEM] Голосовое отменено.", "system");
                }
            });
        });

        // Получение голосового
        eventBus.subscribe(VoiceMessageReadyEvent.class, evt -> {
            appendMessage("[🎤 Голосовое сообщение от " + evt.username() + "]", "system");
            appendPlayButton(evt.data(), evt.username());
        });
        eventBus.subscribe(VoiceLevelEvent.class, e ->
                SwingUtilities.invokeLater(() -> updateRecordStatus(e.level())));

    }

    // ========================== Основная логика ==========================
    private void handleInput() {
        String text = inputField.getText().trim();
        if (text.isEmpty()) return;

        if (text.startsWith("/")) {
            try {
                ctx.getCommandHandler().getReplRunner().execute(text);
            } catch (UnknownCommandException e) {
                appendMessage("[SYSTEM] Unknown command: " + text, "error");
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
                    FileSender.sendFile(file, ctx.getUserInfo().getName(), connector.getOutputStream(), eventBus);
                } catch (Exception ex) {
                    NotificationManager.showError("Ошибка при отправке файла: " + ex.getMessage());
                }
            }).start();
        }
    }

    private void startRecording() {
        if (recording) return;
        recording = true;
        recorder = new VoiceRecorder(ctx);
        new Thread(recorder, "VoiceRecorder").start();
        eventBus.publish(new VoiceRecordingEvent(ctx.getUserInfo().getName(), true));

        recordStartTime = System.currentTimeMillis();
        recordTimer = new Timer(200, e -> updateRecordStatus(0));
        recordTimer.start();
        recordStatusLabel.setText("🎙 Recording...");
        voiceButton.setText("⏹ Stop");
    }

    private void stopRecording() {
        if (!recording) return;
        recording = false;
        if (recorder != null) recorder.stop();
        eventBus.publish(new VoiceRecordingEvent(ctx.getUserInfo().getName(), false));

        if (recordTimer != null) recordTimer.stop();
        recordStatusLabel.setText(" ");
        voiceButton.setText("🎤 Voice");
    }
    private void updateRecordStatus(double level) {
        long seconds = (System.currentTimeMillis() - recordStartTime) / 1000;
        int bar = (int) (level * 10);
        StringBuilder vu = new StringBuilder();
        for (int i = 0; i < 10; i++) vu.append(i < bar ? "█" : " ");
        recordStatusLabel.setText(String.format("🎙 [%s] %02ds", vu, seconds));
    }


    private void toggleTheme() {
        Theme newTheme = themeToggle.isSelected() ? Theme.LIGHT : Theme.DARK;
        ctx.setTheme(newTheme);
        themeToggle.setText(newTheme == Theme.DARK ? "☀️" : "🌙");
        applyTheme(newTheme);
    }

    private void toggleSound() {
        boolean enabled = soundToggle.isSelected();
        ctx.setSoundEnabled(enabled);
        soundToggle.setText(enabled ? "🔊" : "🔈");
    }

    // ========================== UI ==========================
    private void createStyles(JTextPane pane) {
        Style def = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);

        if (pane.getStyle("default") == null) {
            Style regular = pane.addStyle("default", def);
            StyleConstants.setFontFamily(regular, "Consolas");
            StyleConstants.setFontSize(regular, 14);
        }
        if (pane.getStyle("system") == null) pane.addStyle("system", pane.getStyle("default"));
        if (pane.getStyle("error") == null) pane.addStyle("error", pane.getStyle("default"));
    }

    private void appendMessage(String msg, String style) {
        SwingUtilities.invokeLater(() -> {
            try {
                doc.insertString(doc.getLength(), msg + "\n", doc.getStyle(style));
                chatPane.setCaretPosition(doc.getLength());
            } catch (BadLocationException ignored) {}
        });
    }

    private void appendPlayButton(byte[] voiceData, String username) {
        SwingUtilities.invokeLater(() -> {
            JButton playButton = new JButton("▶ " + username);
            playButton.addActionListener(e ->
                    new Thread(() -> new VoicePlayer(voiceData).play(), "VoicePlayer").start()
            );
            chatPane.setCaretPosition(doc.getLength());
            chatPane.insertComponent(playButton);
            try {
                doc.insertString(doc.getLength(), "\n\n", doc.getStyle("default"));
            } catch (BadLocationException ignored) {}
        });
    }

    private void applyTheme(Theme theme) {
        Color bg, fg, sys, err;
        if (theme == Theme.DARK) {
            bg = new Color(25, 25, 25);
            fg = Color.WHITE;
            sys = new Color(180, 180, 180);
            err = new Color(255, 80, 80);
        } else {
            bg = Color.WHITE;
            fg = Color.BLACK;
            sys = new Color(80, 80, 80);
            err = new Color(200, 0, 0);
        }

        chatPane.setBackground(bg);
        inputField.setBackground(bg);
        userList.setBackground(bg);
        chatPane.setForeground(fg);
        inputField.setForeground(fg);
        userList.setForeground(fg);

        // обновляем цвета существующих стилей — старый текст перекрашивается
        StyleConstants.setForeground(chatPane.getStyle("default"), fg);
        StyleConstants.setForeground(chatPane.getStyle("system"), sys);
        StyleConstants.setForeground(chatPane.getStyle("error"), err);

        chatPane.repaint();
    }
}
