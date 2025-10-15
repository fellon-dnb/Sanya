package com.sanya.client.ui.main;

import com.sanya.client.ApplicationContext;
import com.sanya.client.ui.NotificationManager;
import com.sanya.client.ui.dialog.ChatVoiceDialog;
import com.sanya.client.service.audio.VoiceService;
import com.sanya.events.Theme;
import com.sanya.events.ThemeChangedEvent;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.util.List;



/**
 * Основное окно чата: отображение сообщений, пользователей, файлов и голосовых сообщений.
 */
public class ChatMainPanel extends JPanel {

    private final ApplicationContext ctx;
    private final JTextPane chatPane = new JTextPane();
    private final DefaultListModel<String> userListModel = new DefaultListModel<>();
    private final JList<String> userList = new JList<>(userListModel);
    private final JLabel recordStatusLabel = new JLabel(" ");
    private final StyledDocument doc = chatPane.getStyledDocument();
    private Timer recTimer;
    private boolean recBlink;
    public ChatMainPanel(ApplicationContext ctx) {
        this.ctx = ctx;
        setLayout(new BorderLayout());

        // === Создаём базовые стили текста ===
        StyleContext sc = StyleContext.getDefaultStyleContext();

        javax.swing.text.Style def = sc.addStyle("default", null);
        def.addAttribute(javax.swing.text.StyleConstants.FontFamily, "Segoe UI");
        def.addAttribute(javax.swing.text.StyleConstants.FontSize, 13);
        def.addAttribute(javax.swing.text.StyleConstants.Foreground, Color.WHITE);
        doc.addStyle("default", def);

        javax.swing.text.Style sys = sc.addStyle("system", def);
        sys.addAttribute(javax.swing.text.StyleConstants.Foreground, new Color(150, 150, 150));
        doc.addStyle("system", sys);

        // === Строим UI ===
        buildUI();

        // === Подписка на смену темы ===
        ctx.getEventBus().subscribe(ThemeChangedEvent.class, e ->
                SwingUtilities.invokeLater(() -> applyTheme(e.theme())));

        // === Подписка на уровень громкости записи ===
        ctx.getEventBus().subscribe(com.sanya.events.VoiceLevelEvent.class,
                e -> SwingUtilities.invokeLater(() -> updateRecordingLevel(e.level())));
    }



    private void buildUI() {
        // чат
        chatPane.setEditable(false);
        chatPane.setMargin(new Insets(6, 6, 6, 6));
        add(new JScrollPane(chatPane), BorderLayout.CENTER);

        // панель статуса записи (рядом с кнопками ввода)
        JPanel bottomPanel = new JPanel(new BorderLayout());
        recordStatusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        bottomPanel.add(new com.sanya.client.ui.input.ChatInputPanel(ctx), BorderLayout.CENTER);
        bottomPanel.add(recordStatusLabel, BorderLayout.SOUTH);
        add(bottomPanel, BorderLayout.SOUTH);

        // пользователи
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.add(new JLabel("Active users:"), BorderLayout.NORTH);
        rightPanel.add(new JScrollPane(userList), BorderLayout.CENTER);
        rightPanel.setPreferredSize(new Dimension(180, 0));
        add(rightPanel, BorderLayout.EAST);

        // кнопка смены темы
        JButton themeBtn = new JButton("🌓");
        themeBtn.addActionListener(e -> ctx.services().theme().toggle());
        add(themeBtn, BorderLayout.NORTH);
    }


    // === Методы для SwingUIFacade ===
    public void appendChatMessage(String msg) {
        appendText(msg, "default");
    }

    public void appendSystemMessage(String msg) {
        appendText(msg, "system");
    }

    public void clearChat() {
        SwingUtilities.invokeLater(() -> chatPane.setText(""));
    }

    public void updateUserList(List<String> users) {
        SwingUtilities.invokeLater(() -> {
            userListModel.clear();
            users.forEach(userListModel::addElement);
        });
    }

    public void updateFileTransferProgress(String filename, int percent, boolean outgoing) {
        // простая заглушка, можно расширить диалогом
        NotificationManager.showInfo((outgoing ? "Sending " : "Receiving ") + filename + ": " + percent + "%");
    }

    public void fileTransferCompleted(String filename, boolean outgoing) {
        NotificationManager.showInfo((outgoing ? "File sent: " : "File received: ") + filename);
    }

    public void addVoiceMessage(String username, byte[] data) {
        JButton playButton = new JButton("▶ " + username);
        playButton.addActionListener(ev -> ctx.get(VoiceService.class).playTemp(data));
        chatPane.insertComponent(playButton);
        try {
            doc.insertString(doc.getLength(), "\n", null);
        } catch (BadLocationException ignored) {}
    }



    public void applyTheme(Theme theme) {
        SwingUtilities.invokeLater(() -> refreshThemeColors(theme));
    }


    private void appendText(String msg, String style) {
        SwingUtilities.invokeLater(() -> {
            try {
                if (doc.getLength() > 0) doc.insertString(doc.getLength(), "\n", null);
                doc.insertString(doc.getLength(), msg, doc.getStyle(style));
                chatPane.setCaretPosition(doc.getLength());
            } catch (BadLocationException ignored) {}
        });
    }
    private void refreshThemeColors(Theme theme) {
        Color bg = (theme == Theme.DARK) ? Color.BLACK : Color.WHITE;
        Color fg = (theme == Theme.DARK) ? Color.WHITE : Color.BLACK;

        chatPane.setBackground(bg);
        chatPane.setForeground(fg);
        userList.setBackground(bg);
        userList.setForeground(fg);
        recordStatusLabel.setForeground(fg);

        // перекрасить весь текст
        chatPane.selectAll();
        chatPane.setCharacterAttributes(chatPane.getStyle(StyleContext.DEFAULT_STYLE), true);
        chatPane.setForeground(fg);
        chatPane.setBackground(bg);
        chatPane.repaint();
    }
    private void updateRecordingLevel(double level) {
        int percent = (int) (level * 100);
        recordStatusLabel.setText("● REC " + percent + "%");
        recordStatusLabel.setForeground(Color.RED);
    }
    public void setRecordingIndicator(boolean recording) {
        if (recording) {
            if (recTimer == null) {
                recTimer = new Timer(400, e -> {
                    recBlink = !recBlink;
                    recordStatusLabel.setText(recBlink ? "● REC" : "");
                    recordStatusLabel.setForeground(Color.RED);
                });
            }
            if (!recTimer.isRunning()) {
                recTimer.start();
            }
        } else {
            if (recTimer != null && recTimer.isRunning()) {
                recTimer.stop();
            }
            recBlink = false;
            recordStatusLabel.setText("");
            recordStatusLabel.setForeground(getForeground());
        }
    }
}
