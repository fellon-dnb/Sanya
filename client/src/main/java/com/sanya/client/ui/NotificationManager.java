package com.sanya.client.ui;

import javax.swing.*;
import java.awt.*;

/**
 * NotificationManager — утилита для отображения ненавязчивых уведомлений (toast-сообщений)
 * поверх всех окон приложения. Используется для системных, информационных и
 * ошибочных уведомлений в минималистичном стиле.
 *
 * Назначение:
 * - Отображение временных сообщений пользователю без блокировки интерфейса.
 * - Автоматическое исчезновение уведомления через заданный интервал (по умолчанию 3 секунды).
 * - Сохранение минималистичного вида, напоминающего консоль или REPL.
 *
 * Визуальные характеристики:
 * - Окно без рамок, всегда поверх других окон.
 * - Закруглённые углы, мягкая тень, прозрачный фон.
 * - Расположение в правом нижнем углу экрана.
 *
 * Пример:
 * NotificationManager.showInfo("Message sent");
 * NotificationManager.showError("Connection failed");
 */
public final class NotificationManager {

    private NotificationManager() {
    }

    /**
     * Отображает toast-сообщение с указанными параметрами.
     *
     * @param message    текст уведомления
     * @param background цвет фона панели уведомления
     * @param textColor  цвет текста
     */
    public static void showToast(String message, Color background, Color textColor) {
        SwingUtilities.invokeLater(() -> {
            JWindow toast = new JWindow();
            toast.setAlwaysOnTop(true);
            toast.setFocusableWindowState(false);
            toast.setBackground(new Color(0, 0, 0, 0));

            JPanel panel = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    int arc = 20;
                    g2.setColor(background);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);

                    g2.setColor(new Color(0, 0, 0, 60));
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
                    g2.dispose();
                }
            };

            panel.setLayout(new GridBagLayout());
            panel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

            JLabel label = new JLabel(message);
            label.setForeground(textColor);
            label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            panel.add(label);

            toast.add(panel);
            toast.pack();

            Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
            int x = screen.width - toast.getWidth() - 40;
            int y = screen.height - toast.getHeight() - 80;
            toast.setLocation(x, y);

            toast.setVisible(true);

            new Timer(3000, e -> toast.dispose()).start();
        });
    }

    /**
     * Отображает информационное уведомление.
     *
     * @param message текст уведомления
     */
    public static void showInfo(String message) {
        showToast("[INFO] " + message, new Color(45, 45, 45), Color.WHITE);
    }

    /**
     * Отображает предупреждение.
     *
     * @param message текст уведомления
     */
    public static void showWarning(String message) {
        showToast("[WARN] " + message, new Color(120, 80, 0), Color.WHITE);
    }

    /**
     * Отображает уведомление об ошибке.
     *
     * @param message текст уведомления
     */
    public static void showError(String message) {
        showToast("[ERROR] " + message, new Color(219, 15, 15), Color.WHITE);
    }
}
