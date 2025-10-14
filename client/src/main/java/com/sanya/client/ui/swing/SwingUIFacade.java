package com.sanya.client.ui.swing;

import com.sanya.client.ui.UIFacade;
import com.sanya.client.ChatClientUI;
import com.sanya.client.ui.NotificationManager;

import javax.swing.*;
import java.io.File;
import java.util.List;

public class SwingUIFacade implements UIFacade {

    private final ChatClientUI ui;

    public SwingUIFacade(ChatClientUI ui) {
        this.ui = ui;
    }

    @Override
    public void appendChatMessage(String text) {
        ui.appendChatMessage(text); // заменено на публичный метод
    }

    @Override
    public void appendSystemMessage(String text) {
        ui.appendSystemMessage("[SYSTEM] " + text);
    }

    @Override
    public void clearChat() {
        SwingUtilities.invokeLater(ui::clearChat);
    }

    @Override
    public void updateUserList(List<String> usernames) {
        SwingUtilities.invokeLater(() -> ui.updateUserList(usernames));
    }

    @Override
    public void showFileTransferProgress(String filename, int percent, boolean outgoing) {
        ui.updateFileTransferProgress(filename, percent, outgoing);
    }

    @Override
    public void showFileTransferCompleted(String filename, boolean outgoing) {
        ui.fileTransferCompleted(filename, outgoing);
    }

    @Override
    public File askFileToSend() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            return chooser.getSelectedFile();
        }
        return null;
    }

    @Override
    public void showFileSaveDialog(String filename, byte[] data) {
        // Реализовать позже
    }

    @Override
    public void showVoiceMessage(String username, byte[] data) {
        ui.addVoiceMessage(username, data);
    }

    @Override
    public void showVoiceRecordingStatus(boolean recording) {
        ui.setRecordingIndicator(recording);
    }

    @Override
    public void showInfo(String message) {
        NotificationManager.showInfo(message);
    }

    @Override
    public void showWarning(String message) {
        NotificationManager.showWarning(message);
    }

    @Override
    public void showError(String message) {
        NotificationManager.showError(message);
    }

    @Override
    public void applyTheme(Object theme) {
        if (theme instanceof com.sanya.events.Theme t) {
            ui.applyTheme(t);
        }
    }

}