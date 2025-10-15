package com.sanya.client.ui.dialog;

import com.sanya.client.service.audio.VoiceService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class ChatVoiceDialog extends JDialog {
    private final byte[] data;
    private final VoiceService service;
    private JButton sendButton;
    private boolean sent = false;

    public ChatVoiceDialog(JFrame parent, byte[] data, VoiceService service) {
        super(parent, "Голосовое сообщение", true);
        this.data = data;
        this.service = service;

        initializeUI();
        setupWindowListener();
    }

    private void initializeUI() {
        JButton playButton = new JButton("▶ Прослушать");
        sendButton = new JButton("📤 Отправить");
        JButton cancelButton = new JButton("❌ Удалить");

        // Настройка кнопок
        playButton.addActionListener(new PlayButtonListener());
        sendButton.addActionListener(new SendButtonListener());
        cancelButton.addActionListener(new CancelButtonListener());

        // Создание панели
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        buttonPanel.add(playButton);
        buttonPanel.add(sendButton);
        buttonPanel.add(cancelButton);

        // Информационная панель
        JLabel infoLabel = new JLabel("Голосовое сообщение готово к отправке");
        infoLabel.setHorizontalAlignment(SwingConstants.CENTER);

        // Основная компоновка
        setLayout(new BorderLayout(10, 10));
        add(infoLabel, BorderLayout.NORTH);
        add(buttonPanel, BorderLayout.CENTER);

        // Настройка диалога
        pack();
        setLocationRelativeTo(getParent());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);
    }

    private void setupWindowListener() {
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cleanup();
            }
        });
    }

    private void cleanup() {
        sent = true;
        sendButton.setEnabled(false);
    }

    private class PlayButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            service.playTemp(data);
        }
    }

    private class SendButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (sent || service.isSending()) {
                return;
            }

            sent = true;
            sendButton.setEnabled(false);
            sendButton.setText("Отправка...");

            // Отправляем в отдельном потоке
            new Thread(() -> {
                service.sendVoice(data);

                // Закрываем диалог в EDT
                SwingUtilities.invokeLater(() -> {
                    dispose();
                });
            }, "VoiceSender").start();
        }
    }

    private class CancelButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            dispose();
        }
    }
}