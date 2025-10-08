package com.sanya.client.ui;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.ConcurrentHashMap;

public class FileTransferProgressDialog extends JDialog {

    private static final ConcurrentHashMap<String, FileTransferProgressDialog> activeDialogs = new ConcurrentHashMap<>();

    private final JProgressBar progressBar = new JProgressBar(0, 100);
    private final JLabel statusLabel = new JLabel();

    public FileTransferProgressDialog(Frame parent, String filename) {
        super(parent, "Передача файла", false);
        setSize(400, 120);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout(10, 10));

        add(new JLabel("Передача: " + filename), BorderLayout.NORTH);
        add(progressBar, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
        progressBar.setStringPainted(true);

        activeDialogs.put(filename, this);
    }

    public void updateProgress(int percent) {
        progressBar.setValue(percent);
        statusLabel.setText("Прогресс: " + percent + "%");
        if (percent >= 100) {
            dispose();
            activeDialogs.remove(this);
        }
    }

    // ✅ Вот этот метод и нужен для ChatClientUI
    public static void updateGlobalProgress(String filename, int percent) {
        FileTransferProgressDialog dialog = activeDialogs.get(filename);
        if (dialog != null) {
            SwingUtilities.invokeLater(() -> dialog.updateProgress(percent));
        }
    }
}
