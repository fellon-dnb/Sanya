package com.sanya.client.ui.input;

import com.sanya.client.ApplicationContext;
import com.sanya.client.service.audio.VoiceService;
import com.sanya.client.service.files.FileSender;
import com.sanya.events.chat.MessageSendEvent;
import com.sanya.events.system.SystemMessageEvent;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;

/**
 * ChatInputPanel — панель пользовательского ввода для текстовых, файловых и голосовых сообщений.
 * Содержит поле ввода, кнопку отправки текста, кнопку выбора файла и кнопку записи голоса.
 *
 * Назначение:
 * Предоставляет пользователю единый интерфейс для взаимодействия с чатом:
 * ввод текста, отправка файлов и запись голосовых сообщений.
 *
 * Потоковая модель:
 * - Все операции UI выполняются в EDT.
 * - Отправка файлов и голосовых данных выполняется в отдельных потоках.
 *
 * Пример:
 * ChatInputPanel inputPanel = new ChatInputPanel(ctx);
 * frame.add(inputPanel, BorderLayout.SOUTH);
 */
public final class ChatInputPanel extends JPanel {

    /** Поле ввода текстового сообщения. */
    private final JTextField input = new JTextField();

    /** Кнопка для отправки текста. */
    private final JButton send = new JButton("Send");

    /** Кнопка выбора и отправки файла. */
    private final JButton file = new JButton("📎");

    /** Кнопка записи голосового сообщения. */
    private final JButton voice = new JButton("🎤");

    /** Сервис управления голосовыми сообщениями. */
    private final VoiceService voiceService;

    /** Контекст приложения, предоставляющий доступ к сервисам и шине событий. */
    private final ApplicationContext ctx;

    /**
     * Создаёт панель пользовательского ввода.
     *
     * @param ctx контекст приложения, через который выполняется публикация событий и доступ к сервисам
     */
    public ChatInputPanel(ApplicationContext ctx) {
        this.ctx = ctx;
        this.voiceService = ctx.get(VoiceService.class);

        setLayout(new BorderLayout());

        JPanel buttons = new JPanel(new GridLayout(1, 3));
        buttons.add(file);
        buttons.add(send);
        buttons.add(voice);

        add(input, BorderLayout.CENTER);
        add(buttons, BorderLayout.EAST);

        send.addActionListener(e -> sendMessage());
        file.addActionListener(e -> doSendFile());

        setupVoiceButton();
    }

    /**
     * Настраивает кнопку микрофона, обеспечивая удаление старых слушателей
     * (если панель пересоздаётся или добавляется повторно).
     */
    private void setupVoiceButton() {
        for (var ml : voice.getMouseListeners()) {
            voice.removeMouseListener(ml);
        }

        voice.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                voicePressed();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                voiceReleased();
            }
        });
    }

    /**
     * Отправляет текстовое сообщение через EventBus.
     * Публикует событие {@link MessageSendEvent} и очищает поле ввода.
     */
    private void sendMessage() {
        String text = input.getText().trim();
        if (text.isEmpty()) return;

        ctx.getEventBus().publish(new MessageSendEvent(text));
        input.setText("");
    }

    /**
     * Открывает диалог выбора файла и отправляет выбранный файл в отдельном потоке.
     * Использует {@link FileSender}, доступный через {@link ApplicationContext}.
     */
    private void doSendFile() {
        File chosen = ctx.getUIFacade().askFileToSend();
        if (chosen == null) return;

        new Thread(() -> {
            try {
                String recipient = ctx.getUserSettings().getName();
                ctx.services().fileSender().sendFile(
                        recipient,
                        chosen,
                        ctx.services().chat()::sendObject
                );
            } catch (Exception ex) {
                ctx.getEventBus().publish(
                        new SystemMessageEvent("[ERROR] Отправка файла: " + ex.getMessage())
                );
            }
        }, "FileSenderThread").start();
    }

    /**
     * Обрабатывает нажатие на кнопку микрофона.
     * Запускает запись голосового сообщения через {@link VoiceService}.
     */
    private void voicePressed() {
        if (voiceService == null) return;

        voice.setText("⏺ REC");
        voice.setEnabled(false);

        SwingUtilities.invokeLater(() -> {
            voiceService.startRecording();
            voice.setEnabled(true);
        });
    }

    /**
     * Обрабатывает отпускание кнопки микрофона.
     * Останавливает запись голосового сообщения.
     */
    private void voiceReleased() {
        if (voiceService == null) return;

        SwingUtilities.invokeLater(() -> {
            voiceService.stopRecording();
            voice.setText("🎤");
        });
    }
}
