package com.sanya.client.ui;

import javax.swing.*;
import java.awt.*;

/**
 * Минималистичные уведомления в стиле REPL / терминала — всегда поверх всех окон.
 */
public class NotificationManager {

    public static void showToast(String message, Color background, Color textColor) {
        SwingUtilities.invokeLater(() -> {
            // Окно уведомления
            JWindow toast = new JWindow();
            toast.setAlwaysOnTop(true);
            toast.setFocusableWindowState(false);
            toast.setBackground(new Color(0, 0, 0, 0)); // прозрачный фон для окна

            // Контейнер с закруглёнными углами и тенью
            JPanel panel = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    int arc = 20;
                    g2.setColor(background);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);

                    // Лёгкая тень
                    g2.setColor(new Color(0, 0, 0, 60));
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);

                    g2.dispose();
                }
            };

            panel.setLayout(new GridBagLayout());
            panel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

            JLabel label = new JLabel(message);
            label.setForeground(textColor);
            label.setFont(new Font("Segoe UI", Font.PLAIN, 12)); //  читаемый системный шрифт
            panel.add(label);

            toast.add(panel);
            toast.pack();

            // позиция — правый нижний угол экрана
            Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
            int x = screen.width - toast.getWidth() - 40;
            int y = screen.height - toast.getHeight() - 80;
            toast.setLocation(x, y);

            toast.setVisible(true);

            // Таймер скрытия
            new Timer(3000, e -> toast.dispose()).start();
        });
    }

    public static void showInfo(String message) {
        showToast("[INFO] " + message, new Color(45, 45, 45), Color.WHITE);
    }

    public static void showWarning(String message) {
        showToast("[WARN] " + message, new Color(120, 80, 0), Color.WHITE);
    }

    public static void showError(String message) {
        showToast("[ERROR] " + message, new Color(219, 15, 15), Color.WHITE);
    }
}
