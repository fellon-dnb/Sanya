package com.sanya.client;

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

/**
 * Полноценный UI клиента, адаптированный под UIFacade.
 */
public class ChatClientUI extends JFrame {

    private static final int USERS_WIDTH = 180;

    private final ApplicationContext ctx;
    private final JTextPane chatPane = new JTextPane();
    private final DefaultListModel<String> userListModel = new DefaultListModel<>();
    private final JList<String> userList = new JList<>(userListModel);
    private final JTextField inputField = new JTextField();
    private final JButton sendButton = new JButton("Send");
    private final JButton fileButton = new JButton("📁 File");
    private final JButton voiceButton = new JButton("🎤 Voice");
    private final JLabel recordStatusLabel = new JLabel(" ");
    private final StyledDocument doc = chatPane.getStyledDocument();

    private JPanel bottomPanel;
    private VoiceRecorder recorder;
    private boolean recording = false;
    private Timer recordTimer;
    private long recordStartMs = 0;

    public ChatClientUI(ApplicationContext ctx) {
        this.ctx = ctx;

        setTitle("Chat Client - " + ctx.getUserSettings().getName());
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(800, 550);
        setMinimumSize(new Dimension(800, 550));
        setLayout(new BorderLayout());
        setLocationRelativeTo(null);

        buildUI();
        subscribeUIHandlers();
        subscribeEvents();

        // Применяем тему только после построения интерфейса
        SwingUtilities.invokeLater(() -> applyTheme(ctx.getUiSettings().getTheme()));
    }

    /** Построение интерфейса */
    private void buildUI() {
        // Центр — чат
        chatPane.setEditable(false);
        chatPane.setMargin(new Insets(6, 6, 6, 6));
        add(new JScrollPane(chatPane), BorderLayout.CENTER);

        // Правая панель — пользователи
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.add(new JLabel("Active users:"), BorderLayout.NORTH);
        JScrollPane userScroll = new JScrollPane(userList);
        Dimension uw = new Dimension(USERS_WIDTH, 0);
        userScroll.setPreferredSize(uw);
        rightPanel.add(userScroll, BorderLayout.CENTER);
        add(rightPanel, BorderLayout.EAST);

        // Верхняя панель
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
        JToggleButton themeToggle = new JToggleButton(ctx.getUiSettings().getTheme() == Theme.DARK ? "☀️" : "🌙");
        JToggleButton soundToggle = new JToggleButton(ctx.getUiSettings().isSoundEnabled() ? "🔊" : "🔈");
        topPanel.add(themeToggle);
        topPanel.add(soundToggle);
        add(topPanel, BorderLayout.NORTH);

        // Нижняя панель
        bottomPanel = new JPanel(new BorderLayout());
        JPanel buttons = new JPanel(new GridLayout(1, 3));
        buttons.add(fileButton);
        buttons.add(sendButton);
        buttons.add(voiceButton);

        bottomPanel.add(inputField, BorderLayout.CENTER);
        bottomPanel.add(buttons, BorderLayout.EAST);
        bottomPanel.add(recordStatusLabel, BorderLayout.WEST);
        add(bottomPanel, BorderLayout.SOUTH);

        // Обработчики верхних переключателей
        themeToggle.addActionListener(e -> toggleTheme(themeToggle));
        soundToggle.addActionListener(e -> toggleSound(soundToggle));
    }

    /** Обработчики UI */
    private void subscribeUIHandlers() {
        sendButton.addActionListener(e -> handleInput());
        inputField.addActionListener(e -> handleInput());
        fileButton.addActionListener(e -> handleFileSend());

        voiceButton.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { startRecording(); }
            @Override public void mouseReleased(MouseEvent e) { stopRecording(); }
        });
    }

    /** Подписка на события */
    private void subscribeEvents() {
        EventBus bus = ctx.getEventBus();

        bus.subscribe(MessageReceivedEvent.class, e -> appendChatMessage(e.message().toString()));
        bus.subscribe(UserListUpdatedEvent.class, e -> updateUserList(e.usernames()));
        bus.subscribe(ClearChatEvent.class, e -> clearChat());
        bus.subscribe(VoiceLevelEvent.class, e -> SwingUtilities.invokeLater(() -> updateRecordStatus(e.level())));

        bus.subscribe(VoiceRecordingStoppedEvent.class, e -> {
            byte[] audio = e.data();

            SwingUtilities.invokeLater(() -> {
                JDialog dialog = new JDialog(this, "Голосовое сообщение", true);
                dialog.setLayout(new FlowLayout());
                dialog.setSize(300, 120);
                dialog.setLocationRelativeTo(this);

                JButton play = new JButton("▶ Прослушать");
                JButton send = new JButton("📤 Отправить");
                JButton delete = new JButton("🗑 Удалить");

                play.addActionListener(ev -> new Thread(() -> new com.sanya.client.audio.VoicePlayer(audio).play()).start());
                delete.addActionListener(ev -> dialog.dispose());

                send.addActionListener(ev -> {
                    dialog.dispose();
                    new Thread(() -> {
                        try {
                            var out = ctx.services().chat().getOutputStream();
                            var req = new com.sanya.files.FileTransferRequest(
                                    ctx.getUserSettings().getName(),
                                    "voice",
                                    audio.length
                            );
                            out.writeObject(req);
                            out.flush();

                            var chunk = new com.sanya.files.FileChunk("voice", audio, 0, true);
                            out.writeObject(chunk);
                            out.flush();

                            appendVoiceMessageSelf(audio);
                        } catch (Exception ex) {
                            NotificationManager.showError("Ошибка отправки: " + ex.getMessage());
                        }
                    }).start();
                });

                dialog.add(play);
                dialog.add(send);
                dialog.add(delete);
                dialog.setVisible(true);
            });
        });

        bus.subscribe(VoiceMessageReadyEvent.class, e -> {
            SwingUtilities.invokeLater(() -> {
                appendSystemMessage("[🎤 Голосовое сообщение от " + e.username() + "]");
                appendPlayButton(e.data(), e.username());
            });
        });
    }

    private void appendVoiceMessageSelf(byte[] data) {
        SwingUtilities.invokeLater(() -> {
            appendSystemMessage("[🎤 Вы отправили голосовое сообщение]");
            appendPlayButton(data, "Вы");
        });}
    // === Методы для UIFacade ===
    public void appendChatMessage(String text) {
        appendMessage(text, "default");
    }

    public void appendSystemMessage(String text) {
        appendMessage(text, "system");
    }

    public void clearChat() {
        SwingUtilities.invokeLater(() -> chatPane.setText(""));
    }

    public void updateUserList(java.util.List<String> usernames) {
        SwingUtilities.invokeLater(() -> {
            userListModel.clear();
            usernames.forEach(userListModel::addElement);
        });
    }

    public void updateFileTransferProgress(String filename, int percent, boolean outgoing) {
        FileTransferProgressDialog.updateGlobalProgress(filename, percent);
    }

    public void fileTransferCompleted(String filename, boolean outgoing) {
        NotificationManager.showInfo((outgoing ? "Отправка" : "Приём") + " файла " + filename + " завершена");
        FileTransferProgressDialog.close(filename);
    }

    public void addVoiceMessage(String username, byte[] data) {
        appendMessage("[🎤 Голосовое сообщение от " + username + "]", "system");
    }

    public void setRecordingIndicator(boolean recording) {
        recordStatusLabel.setText(recording ? "● REC" : " ");
    }

    public void applyTheme(Theme theme) {
        Color bg = (theme == Theme.DARK) ? Color.BLACK : Color.WHITE;
        Color fg = (theme == Theme.DARK) ? Color.WHITE : Color.BLACK;
        Color panelBg = (theme == Theme.DARK) ? Color.DARK_GRAY : Color.LIGHT_GRAY;

        chatPane.setBackground(bg);
        chatPane.setForeground(fg);
        userList.setBackground(bg);
        userList.setForeground(fg);
        if (bottomPanel != null) bottomPanel.setBackground(panelBg);

        recordStatusLabel.setForeground(fg);
        inputField.setBackground(bg);
        inputField.setForeground(fg);

        if (bottomPanel != null) {
            for (Component c : bottomPanel.getComponents()) {
                if (c instanceof JButton b) {
                    b.setBackground(panelBg);
                    b.setForeground(fg);
                }
            }
        }

        SwingUtilities.invokeLater(chatPane::repaint);
    }

    // === Логика чата ===
    private void handleInput() {
        String text = inputField.getText().trim();
        if (text.isEmpty()) return;
        ctx.getEventBus().publish(new MessageSendEvent(text));
        inputField.setText("");
    }

    private void handleFileSend() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            new Thread(() -> {
                FileTransferProgressDialog.open(this, file.getName(), true);
                try {
                    FileSender.sendFile(file, ctx.getUserSettings().getName(), ctx.services().chat().getOutputStream(), ctx.getEventBus());
                } catch (Exception ex) {
                    NotificationManager.showError("Ошибка отправки файла: " + ex.getMessage());
                }
            }).start();
        }
    }

    // === Голосовые сообщения ===
    private void startRecording() {
        if (recording) return;
        recording = true;
        recorder = new VoiceRecorder(ctx);
        new Thread(recorder, "VoiceRecorder").start();
        ctx.getEventBus().publish(new VoiceRecordingEvent(ctx.getUserSettings().getName(), true));
        setRecordingIndicator(true);

        recordStartMs = System.currentTimeMillis();
        recordTimer = new Timer(1000, e -> updateRecordStatus(0));
        recordTimer.start();
    }

    private void stopRecording() {
        if (!recording) return;
        recording = false;

        if (recorder != null) recorder.stop();
        if (recordTimer != null) recordTimer.stop();

        ctx.getEventBus().publish(new VoiceRecordingEvent(ctx.getUserSettings().getName(), false));
        setRecordingIndicator(false);
    }

    private void updateRecordStatus(double level) {
        long seconds = (System.currentTimeMillis() - recordStartMs) / 1000;
        int bar = (int) (level * 10);
        StringBuilder vu = new StringBuilder();
        for (int i = 0; i < 10; i++) vu.append(i < bar ? "█" : " ");
        recordStatusLabel.setText(String.format("🎙 [%s] %02ds", vu, seconds));
    }

    // === Переключатели ===
    private void toggleTheme(JToggleButton themeToggle) {
        Theme newTheme = ctx.getUiSettings().getTheme() == Theme.DARK ? Theme.LIGHT : Theme.DARK;
        ctx.getUiSettings().setTheme(newTheme);
        themeToggle.setText(newTheme == Theme.DARK ? "☀️" : "🌙");
        applyTheme(newTheme);
        ctx.getEventBus().publish(new ThemeChangedEvent(newTheme));
    }

    private void toggleSound(JToggleButton soundToggle) {
        boolean enabled = !ctx.getUiSettings().isSoundEnabled();
        ctx.getUiSettings().setSoundEnabled(enabled);
        soundToggle.setText(enabled ? "🔊" : "🔈");
    }

    // === Утилита ===
    private void appendMessage(String msg, String style) {
        SwingUtilities.invokeLater(() -> {
            try {
                doc.insertString(doc.getLength(), msg + "\n", doc.getStyle(style));
                chatPane.setCaretPosition(doc.getLength());
            } catch (BadLocationException ignored) {}
        });
    }

    private void appendPlayButton(byte[] voiceData, String username) {
        JButton playButton = new JButton("▶ " + username);
        playButton.addActionListener(ev ->
                new Thread(() -> new com.sanya.client.audio.VoicePlayer(voiceData).play()).start()
        );

        chatPane.insertComponent(playButton);
        try {
            doc.insertString(doc.getLength(), "\n\n", doc.getStyle("default"));
        } catch (BadLocationException ignored) {}
    }
}
