package com.sanya.client.ui.dialog;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Диалог отображения прогресса передачи файлов (загрузка/отправка).
 * Управляется через события FileTransferEvent.
 * Безопасен для многопоточности и не вызывает рекурсивных обновлений.
 */
public class FileTransferProgressDialog extends JDialog {

    // Активные окна прогресса, одно окно на каждый файл
    private static final ConcurrentHashMap<String, FileTransferProgressDialog> activeDialogs = new ConcurrentHashMap<>();

    private final JProgressBar progressBar = new JProgressBar(0, 100);
    private final JLabel statusLabel = new JLabel();
    private final String filename;

    public FileTransferProgressDialog(Frame parent, String filename, boolean outgoing) {
        super(parent, (outgoing ? "Отправка" : "Загрузка") + ": " + filename, false);
        this.filename = filename;

        setSize(400, 120);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout(10, 10));

        add(new JLabel((outgoing ? "Отправляется" : "Принимается") + " файл: " + filename), BorderLayout.NORTH);
        add(progressBar, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
        progressBar.setStringPainted(true);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        // ❌ Ранее здесь было activeDialogs.put(filename, this) — теперь не нужно
    }

    /** Обновление прогресса конкретного окна */
    public void updateProgress(int percent) {
        progressBar.setValue(percent);
        statusLabel.setText("Прогресс: " + percent + "%");

        if (percent >= 100) {
            dispose();
            activeDialogs.remove(filename);
        }
    }

    /** Открытие окна, если оно ещё не создано */
    public static FileTransferProgressDialog open(Frame parent, String filename, boolean outgoing) {
        return activeDialogs.computeIfAbsent(filename, f -> {
            FileTransferProgressDialog dialog = new FileTransferProgressDialog(parent, f, outgoing);
            SwingUtilities.invokeLater(() -> dialog.setVisible(true));
            return dialog;
        });
    }

    /** Глобальное обновление окна по имени файла */
    public static void updateGlobalProgress(String filename, int percent) {
        FileTransferProgressDialog dialog = activeDialogs.get(filename);
        if (dialog != null) {
            SwingUtilities.invokeLater(() -> dialog.updateProgress(percent));
        }
    }

    /** Закрытие окна */
    public static void close(String filename) {
        FileTransferProgressDialog dialog = activeDialogs.remove(filename);
        if (dialog != null) {
            SwingUtilities.invokeLater(dialog::dispose);
        }
    }
}
