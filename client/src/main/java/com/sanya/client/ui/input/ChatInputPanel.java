package com.sanya.client.ui.input;

import com.sanya.client.ApplicationContext;
import com.sanya.client.service.audio.VoiceService;
import com.sanya.events.MessageSendEvent;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ChatInputPanel extends JPanel {
    private final JTextField input = new JTextField();
    private final JButton send = new JButton("Send");
    private final JButton file = new JButton("📁");
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
        voice.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { voiceService.startRecording(); }
            @Override public void mouseReleased(MouseEvent e) { voiceService.stopRecording(); }
        });
    }

    private void sendMessage() {
        String text = input.getText().trim();
        if (text.isEmpty()) return;
        ctx.getEventBus().publish(new MessageSendEvent(text));
        input.setText("");
    }
}
