package com.sanya.client.ui;

import javax.swing.*;
import java.awt.*;

public class NotificationManager {
    public static void showToast(String message, Color background, Color textColor) {
        JWindow toast = new JWindow();
        JLabel label = new JLabel(message);
        label.setOpaque(true);
        label.setBackground(background);
        label.setForeground(textColor);
        label.setBorder(BorderFactory.createEmptyBorder(10,20,10,20));
        toast.add(label);
        toast.pack();

        // Позиционирование — правый нижний угол
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int x = screen.width - toast.getWidth() - 30;
        int y = screen.height - toast.getHeight() - 50;
        toast.setLocation(x, y);

        // Показываем
        toast.setAlwaysOnTop(true);
        toast.setVisible(true);

        // Таймер скрытия
        new Timer(2500, e -> toast.dispose()).start();
    }

    public static void showInfo(String message) {
        showToast("💬 " + message, new Color(50, 50, 50, 230), Color.WHITE);
    }

    public static void showWarning(String message) {
        showToast("⚠️ " + message, new Color(120, 80, 0, 230), Color.WHITE);
    }

    public static void showError(String message) {
        showToast("❌ " + message, new Color(120, 0, 0, 230), Color.WHITE);
    }
}


