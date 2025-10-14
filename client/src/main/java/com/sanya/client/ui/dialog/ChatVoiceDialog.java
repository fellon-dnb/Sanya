package com.sanya.client.ui.dialog;

import com.sanya.client.service.audio.VoiceService;

import javax.swing.*;
import java.awt.*;

public class ChatVoiceDialog extends JDialog {
    public ChatVoiceDialog(JFrame parent, byte[] data, VoiceService service) {
        super(parent, "Голосовое сообщение", true);
        JButton play = new JButton("▶ Прослушать");
        JButton send = new JButton("📤 Отправить");
        JButton cancel = new JButton("✖ Отмена");

        play.addActionListener(e -> service.playTemp(data));
        send.addActionListener(e -> {
            service.sendVoice(data);
            dispose();
        });
        cancel.addActionListener(e -> dispose());

        setLayout(new FlowLayout());
        add(play);
        add(send);
        add(cancel);
        pack();
        setLocationRelativeTo(parent);
    }
}

