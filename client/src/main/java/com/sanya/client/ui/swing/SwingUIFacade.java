package com.sanya.client.ui.swing;

import com.sanya.client.ApplicationContext;
import com.sanya.client.ui.UIFacade;
import com.sanya.client.ui.main.ChatMainPanel;
import com.sanya.client.ui.NotificationManager;
import com.sanya.events.SystemMessageEvent;
import com.sanya.events.Theme;
import com.sanya.events.SystemInfoEvent;

import javax.swing.*;
import java.io.File;
import java.util.List;

/**
 * Swing-реализация фасада для UI.
 * Работает с ChatMainPanel вместо ChatClientUI.
 */
public class SwingUIFacade implements UIFacade {

    private final ApplicationContext ctx;
    private final ChatMainPanel mainPanel;

    public SwingUIFacade(ApplicationContext ctx, ChatMainPanel mainPanel) {
        this.ctx = ctx;
        this.mainPanel = mainPanel;

        ctx.getEventBus().subscribe(SystemMessageEvent.class,
                e -> NotificationManager.showError(e.message()));
        ctx.getEventBus().subscribe(SystemInfoEvent.class,
                e -> NotificationManager.showInfo(e.message()));
    }

    @Override
    public void appendChatMessage(String text) {
        mainPanel.appendChatMessage(text);
    }

    @Override
    public void appendSystemMessage(String text) {
        mainPanel.appendSystemMessage("[SYSTEM] " + text);
    }

    @Override
    public void clearChat() {
        SwingUtilities.invokeLater(mainPanel::clearChat);
    }

    @Override
    public void updateUserList(List<String> usernames) {
        SwingUtilities.invokeLater(() -> mainPanel.updateUserList(usernames));
    }

    @Override
    public void showFileTransferProgress(String filename, int percent, boolean outgoing) {
        mainPanel.updateFileTransferProgress(filename, percent, outgoing);
    }

    @Override
    public void showFileTransferCompleted(String filename, boolean outgoing) {
        mainPanel.fileTransferCompleted(filename, outgoing);
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
        // пока не реализовано
    }

    @Override
    public void showVoiceMessage(String username, byte[] data) {
        mainPanel.addVoiceMessage(username, data);
    }

    @Override
    public void showVoiceRecordingStatus(boolean recording) {
        mainPanel.setRecordingIndicator(recording);
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
        if (theme instanceof Theme t) {
            mainPanel.applyTheme(t);
        }
    }
}
