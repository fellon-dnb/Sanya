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

    private final ApplicationContext ctx;
    private final EventBus eventBus;
    private final ChatClientConnector connector;
    private final StyledDocument doc;

    private VoiceRecorder recorder;
    private boolean recording = false;

    // --- UI элементы, связанные с записью
    private JPanel bottomPanel;
    private final JLabel recordStatusLabel = new JLabel(" ");
    private javax.swing.Timer recordTimer;
    private long recordStartMs = 0;


    public ChatClientUI(ApplicationContext ctx) {
        this.ctx = ctx;
        this.eventBus = ctx.getEventBus();

        setTitle("Chat Client - " + ctx.getUserSettings().getName());
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(800, 550);
        setMinimumSize(new Dimension(800, 550));
        setLayout(new BorderLayout());
        setLocationRelativeTo(null);

        //нижния панель
        bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(inputField, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new GridLayout(1, 3));
        buttons.add(fileButton);
        buttons.add(sendButton);
        buttons.add(voiceButton);
        bottomPanel.add(buttons, BorderLayout.EAST);

        add(bottomPanel, BorderLayout.SOUTH);

// статус записи слева, по умолчанию скрыт
        recordStatusLabel.setVisible(false);
        bottomPanel.add(recordStatusLabel, BorderLayout.WEST);


        // Верхняя панель
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
        themeToggle.setText(ctx.getUiSettings().getTheme() == Theme.DARK ? "☀️" : "🌙");
        soundToggle.setText(ctx.getUiSettings().isSoundEnabled() ? "🔊" : "🔈");
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
                ctx.getNetworkSettings().getHost(),
                ctx.getNetworkSettings().getPort(),
                ctx.getUserSettings().getName(),
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

        applyTheme(ctx.getUiSettings().getTheme());
    }

    // ========================== События ==========================
    private void subscribeEvents() {

        ctx.services().chat().onMessageReceived(msg -> {
            appendMessage(msg.toString(), "default");
            if (ctx.getUiSettings().isSoundEnabled()) SoundPlayer.playMessageSound();
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
            if (!e.username().equals(ctx.getUserSettings().getName())) {
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
            ctx.services().chat().sendMessage(text);
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
                    FileSender.sendFile(file, ctx.getUserSettings().getName(), connector.getOutputStream(), eventBus);
                } catch (Exception ex) {
                    NotificationManager.showError("Ошибка при отправке файла: " + ex.getMessage());
                }
            }).start();
        }
    }

    private void startRecording() {
        if (recording) return;
        recording = true;

        // запуск рекордера
        recorder = new VoiceRecorder(ctx);
        new Thread(recorder, "VoiceRecorder").start();
        eventBus.publish(new VoiceRecordingEvent(ctx.getUserSettings().getName(), true));

        // UI: показать статус и таймер
        recordStartMs = System.currentTimeMillis();
        recordStatusLabel.setText("● REC 00:00");
        recordStatusLabel.setVisible(true);
        bottomPanel.revalidate();
        bottomPanel.repaint();

        // подпись на кнопке
        voiceButton.setText("■ Stop");

        // тикать каждую секунду
        if (recordTimer != null) recordTimer.stop();
        recordTimer = new javax.swing.Timer(1000, e -> {
            long sec = (System.currentTimeMillis() - recordStartMs) / 1000;
            recordStatusLabel.setText("● REC " + formatSec(sec));
        });
        recordTimer.start();
    }


    private void stopRecording() {
        if (!recording) return;
        recording = false;

        if (recorder != null) recorder.stop();
        if (recordTimer != null) recordTimer.stop();

        eventBus.publish(new VoiceRecordingEvent(ctx.getUserSettings().getName(), false));

        // UI: убрать статус
        recordStatusLabel.setVisible(false);
        recordStatusLabel.setText("");
        bottomPanel.revalidate();
        bottomPanel.repaint();

        voiceButton.setText("🎤 Voice");
    }

    private void updateRecordStatus(double level) {
        long seconds = (System.currentTimeMillis() - recordStartMs) / 1000;
        int bar = (int) (level * 10);
        StringBuilder vu = new StringBuilder();
        for (int i = 0; i < 10; i++) vu.append(i < bar ? "█" : " ");
        recordStatusLabel.setText(String.format("🎙 [%s] %02ds", vu, seconds));
    }


    private void toggleTheme() {
        Theme newTheme = themeToggle.isSelected() ? Theme.LIGHT : Theme.DARK;
        ctx.getUiSettings().setTheme(newTheme);
        themeToggle.setText(newTheme == Theme.DARK ? "☀️" : "🌙");
        applyTheme(newTheme);
    }

    private void toggleSound() {
        boolean enabled = soundToggle.isSelected();
        ctx.getUiSettings().setSoundEnabled(enabled);
        soundToggle.setText(enabled ? "🔊" : "🔈");
    }
    private static String formatSec(long s) {
        long mm = s / 60;
        long ss = s % 60;
        return String.format("%02d:%02d", mm, ss);
    }
    // ========================== UI ==========================
    private void createStyles(JTextPane pane) {
        StyledDocument doc = pane.getStyledDocument();
        Style defaultStyle = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);
        Style regular = doc.addStyle("default", defaultStyle);
        StyleConstants.setForeground(regular, pane.getForeground());
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
        chatPane.setBackground(theme == Theme.DARK ? Color.BLACK : Color.WHITE);
        chatPane.setForeground(theme == Theme.DARK ? Color.WHITE : Color.BLACK);
        userList.setBackground(theme == Theme.DARK ? Color.BLACK : Color.WHITE);
        userList.setForeground(theme == Theme.DARK ? Color.WHITE : Color.BLACK);
        bottomPanel.setBackground(theme == Theme.DARK ? Color.DARK_GRAY : Color.LIGHT_GRAY);
        recordStatusLabel.setForeground(theme == Theme.DARK ? Color.WHITE : Color.BLACK);

        // Пересоздаём стили, чтобы старые сообщения перекрасились
        createStyles(chatPane);
        SwingUtilities.invokeLater(() -> {
            Style style = chatPane.getStyle("default");
            if (style != null) {
                doc.setCharacterAttributes(0, doc.getLength(), style, false);
            }
            chatPane.repaint();
        });
    }

}
