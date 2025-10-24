package com.sanya.client.ui.main;

import com.sanya.client.ApplicationContext;
import com.sanya.client.ui.NotificationManager;
import com.sanya.client.service.audio.VoiceService;
import com.sanya.events.system.Theme;
import com.sanya.events.system.ThemeChangedEvent;
import com.sanya.events.voice.VoiceLevelEvent;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.util.List;

/**
 * ChatMainPanel — основная панель пользовательского интерфейса чата.
 * Отвечает за визуализацию сообщений, списка пользователей, файлов и голосовых сообщений.
 *
 * Назначение:
 * - Отображает историю сообщений, список активных пользователей и панель ввода.
 * - Реагирует на системные события: смену темы, уровень записи микрофона, окончание записи.
 * - Поддерживает интеграцию с {@link com.sanya.client.facade.swing.SwingUIFacade}.
 *
 * Архитектура:
 * - Центральная область: {@link JTextPane} для сообщений.
 * - Правая панель: {@link JList} со списком активных пользователей.
 * - Нижняя панель: {@link com.sanya.client.ui.input.ChatInputPanel} и индикатор записи.
 * - Верхняя панель: кнопка смены темы оформления.
 *
 * Потоковая модель:
 * Все операции с UI выполняются в EDT через {@link SwingUtilities#invokeLater(Runnable)}.
 *
 * Пример использования:
 * ChatMainPanel panel = new ChatMainPanel(ctx);
 * frame.add(panel, BorderLayout.CENTER);
 */
public final class ChatMainPanel extends JPanel {

    /** Контекст приложения, обеспечивающий доступ к сервисам и шине событий. */
    private final ApplicationContext ctx;

    /** Область чата для отображения сообщений. */
    private final JTextPane chatPane = new JTextPane();

    /** Модель списка пользователей. */
    private final DefaultListModel<String> userListModel = new DefaultListModel<>();

    /** Элемент UI — список активных пользователей. */
    private final JList<String> userList = new JList<>(userListModel);

    /** Метка статуса записи звука. */
    private final JLabel recordStatusLabel = new JLabel(" ");

    /** Документ для форматированного текста. */
    private final StyledDocument doc = chatPane.getStyledDocument();

    /** Таймер мигания индикатора записи. */
    private Timer recTimer;

    /** Флаг состояния мигания. */
    private boolean recBlink;

    /** Конструктор панели. */
    public ChatMainPanel(ApplicationContext ctx) {
        this.ctx = ctx;
        setLayout(new BorderLayout());

        // Настройка базовых стилей текста
        StyleContext sc = StyleContext.getDefaultStyleContext();

        javax.swing.text.Style def = sc.addStyle("default", null);
        def.addAttribute(javax.swing.text.StyleConstants.FontFamily, "Segoe UI");
        def.addAttribute(javax.swing.text.StyleConstants.FontSize, 13);
        def.addAttribute(javax.swing.text.StyleConstants.Foreground, Color.WHITE);
        doc.addStyle("default", def);

        javax.swing.text.Style sys = sc.addStyle("system", def);
        sys.addAttribute(javax.swing.text.StyleConstants.Foreground, new Color(150, 150, 150));
        doc.addStyle("system", sys);

        buildUI();

        // Подписка на смену темы
        ctx.getEventBus().subscribe(ThemeChangedEvent.class,
                e -> SwingUtilities.invokeLater(() -> applyTheme(e.theme())));

        // Подписка на уровень громкости записи
        ctx.getEventBus().subscribe(VoiceLevelEvent.class,
                e -> SwingUtilities.invokeLater(() -> updateRecordingLevel(e.level())));
    }

    /** Строит пользовательский интерфейс панели. */
    private void buildUI() {
        chatPane.setEditable(false);
        chatPane.setMargin(new Insets(6, 6, 6, 6));
        add(new JScrollPane(chatPane), BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        recordStatusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        bottomPanel.add(new com.sanya.client.ui.input.ChatInputPanel(ctx), BorderLayout.CENTER);
        bottomPanel.add(recordStatusLabel, BorderLayout.SOUTH);
        add(bottomPanel, BorderLayout.SOUTH);

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.add(new JLabel("Active users:"), BorderLayout.NORTH);
        rightPanel.add(new JScrollPane(userList), BorderLayout.CENTER);
        rightPanel.setPreferredSize(new Dimension(180, 0));
        add(rightPanel, BorderLayout.EAST);

        JButton themeBtn = new JButton("🌓");
        themeBtn.addActionListener(e -> ctx.services().theme().toggle());
        add(themeBtn, BorderLayout.NORTH);
    }

    /** Добавляет сообщение в чат. */
    public void appendChatMessage(String msg) {
        appendText(msg, "default");
    }

    /** Добавляет системное сообщение в чат. */
    public void appendSystemMessage(String msg) {
        appendText(msg, "system");
    }

    /** Очищает историю сообщений. */
    public void clearChat() {
        SwingUtilities.invokeLater(() -> chatPane.setText(""));
    }

    /** Обновляет список активных пользователей. */
    public void updateUserList(List<String> users) {
        SwingUtilities.invokeLater(() -> {
            userListModel.clear();
            users.forEach(userListModel::addElement);
        });
    }

    /** Отображает текущий прогресс передачи файла. */
    public void updateFileTransferProgress(String filename, int percent, boolean outgoing) {
        NotificationManager.showInfo((outgoing ? "Sending " : "Receiving ") + filename + ": " + percent + "%");
    }

    /** Отображает уведомление об окончании передачи файла. */
    public void fileTransferCompleted(String filename, boolean outgoing) {
        NotificationManager.showInfo((outgoing ? "File sent: " : "File received: ") + filename);
    }

    /** Добавляет в чат кнопку воспроизведения голосового сообщения. */
    public void addVoiceMessage(String username, byte[] data) {
        JButton playButton = new JButton("▶ " + username);
        playButton.addActionListener(ev -> ctx.get(VoiceService.class).playTemp(data));
        chatPane.insertComponent(playButton);
        try {
            doc.insertString(doc.getLength(), "\n", null);
        } catch (BadLocationException ignored) {}
    }

    /** Применяет выбранную тему оформления. */
    public void applyTheme(Theme theme) {
        SwingUtilities.invokeLater(() -> refreshThemeColors(theme));
    }

    /** Добавляет текст в окно чата с указанным стилем. */
    private void appendText(String msg, String style) {
        SwingUtilities.invokeLater(() -> {
            try {
                if (doc.getLength() > 0) doc.insertString(doc.getLength(), "\n", null);
                doc.insertString(doc.getLength(), msg, doc.getStyle(style));
                chatPane.setCaretPosition(doc.getLength());
            } catch (BadLocationException ignored) {}
        });
    }

    /** Обновляет цвета элементов в зависимости от текущей темы. */
    private void refreshThemeColors(Theme theme) {
        Color bg = (theme == Theme.DARK) ? Color.BLACK : Color.WHITE;
        Color fg = (theme == Theme.DARK) ? Color.WHITE : Color.BLACK;

        chatPane.setBackground(bg);
        chatPane.setForeground(fg);
        userList.setBackground(bg);
        userList.setForeground(fg);
        recordStatusLabel.setForeground(fg);

        chatPane.selectAll();
        chatPane.setCharacterAttributes(chatPane.getStyle(StyleContext.DEFAULT_STYLE), true);
        chatPane.setForeground(fg);
        chatPane.setBackground(bg);
        chatPane.repaint();
    }

    /** Обновляет визуальный индикатор уровня громкости записи микрофона. */
    private void updateRecordingLevel(double level) {
        int percent = (int) (level * 100);
        recordStatusLabel.setText("● REC " + percent + "%");
        recordStatusLabel.setForeground(Color.RED);
    }

    /** Управляет мигающим индикатором записи. */
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
