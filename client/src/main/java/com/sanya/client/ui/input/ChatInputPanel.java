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
 * Панель ввода сообщений и кнопок отправки, включая голосовые сообщения.
 */
public class ChatInputPanel extends JPanel {

    private final JTextField input = new JTextField();
    private final JButton send = new JButton("Send");
    private final JButton file = new JButton("📎");
    private final JButton voice = new JButton("🎤");

    private final VoiceService voiceService;
    private final ApplicationContext ctx;

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

    /** Настройка кнопки микрофона с защитой от дублирования слушателей */
    private void setupVoiceButton() {
        // Удаляем старые MouseListener, если панель пересоздаётся
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

    /** Отправка текстового сообщения */
    private void sendMessage() {
        String text = input.getText().trim();
        if (text.isEmpty()) return;

        ctx.getEventBus().publish(new MessageSendEvent(text));
        input.setText("");
    }

    /** Отправка файла */
    private void doSendFile() {
        File chosen = ctx.getUIFacade().askFileToSend();
        if (chosen == null) return;

        new Thread(() -> {
            try {
                FileSender.sendFile(
                        chosen,
                        ctx.getUserSettings().getName(),
                        ctx.services().chat()::sendObject,
                        ctx.getEventBus()
                );
            } catch (Exception ex) {
                ctx.getEventBus().publish(
                        new SystemMessageEvent("[ERROR] Отправка файла: " + ex.getMessage())
                );
            }
        }, "FileSenderThread").start();
    }

    /** Нажатие на микрофон — начало записи */
    private void voicePressed() {
        if (voiceService == null) return;

        voice.setText("⏺ REC");
        voice.setEnabled(false); // защита от повторных кликов
        SwingUtilities.invokeLater(() -> {
            voiceService.startRecording();
            voice.setEnabled(true);
        });
    }

    /** Отпускание кнопки микрофона — завершение записи */
    private void voiceReleased() {
        if (voiceService == null) return;

        SwingUtilities.invokeLater(() -> {
            voiceService.stopRecording();
            voice.setText("🎤");
        });
    }
}
