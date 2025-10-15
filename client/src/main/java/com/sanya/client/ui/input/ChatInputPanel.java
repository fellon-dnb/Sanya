package com.sanya.client.ui.input;

import com.sanya.client.ApplicationContext;
import com.sanya.client.service.audio.VoiceService;
import com.sanya.events.MessageSendEvent;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;

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

        // режим «зажал — запись; отпустил — стоп и диалог»
        voice.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { voicePressed(); }
            @Override public void mouseReleased(MouseEvent e) { voiceReleased(); }
        });
    }

    private void sendMessage() {
        String text = input.getText().trim();
        if (text.isEmpty()) return;
        ctx.getEventBus().publish(new MessageSendEvent(text));
        input.setText("");
    }

    private void doSendFile() {
        File chosen = ctx.getUIFacade().askFileToSend();
        if (chosen == null) return;
        new Thread(() -> {
            try {
                com.sanya.client.files.FileSender.sendFile(
                        chosen,
                        ctx.getUserSettings().getName(),
                        ctx.services().chat().getOutputStream(),
                        ctx.getEventBus()
                );
            } catch (Exception ex) {
                ctx.getEventBus().publish(
                        new com.sanya.events.SystemMessageEvent("[ERROR] Отправка файла: " + ex.getMessage())
                );
            }
        }, "FileSenderThread").start();
    }

    private void voicePressed() {
        voice.setText("⏺ REC");
        voiceService.startRecording();
    }

    private void voiceReleased() {
        // Добавляем защиту от множественного вызова
        SwingUtilities.invokeLater(() -> {
            voiceService.stopRecording();
            voice.setText("🎤");
        });
    }
}
