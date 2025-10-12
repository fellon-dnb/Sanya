package com.sanya.client.ui.swing;

import com.sanya.client.ApplicationContext;          // +++
import com.sanya.client.ui.ChatClientController;
import com.sanya.client.ui.UIFacade;
import com.sanya.events.Theme;
import com.sanya.events.ThemeChangedEvent;           // +++
import com.sanya.files.FileTransferEvent;           // +++
import com.sanya.client.files.FileSender;           // +++
import javax.swing.*;
import java.awt.*;
import java.io.File;                                 // +++
import java.util.List;

public class SwingUIFacade implements UIFacade {

    private final ApplicationContext context;        // +++
    private final ChatClientController controller;
    private final JFrame mainFrame;
    private final JList<String> usersListComponent;
    private final DefaultListModel<String> userListModel;
    private final JTextArea chatArea;

    public SwingUIFacade(ApplicationContext context,  // +++
                         ChatClientController controller) {
        this.context = context;                      // +++
        this.controller = controller;
        ...
        themeButton.addActionListener(e -> {
            Theme current = context.getCurrentTheme();
            Theme next = (current == Theme.DARK) ? Theme.LIGHT : Theme.DARK;
            context.setCurrentTheme(next);
            context.getEventBus().publish(new ThemeChangedEvent(next));
            themeButton.setText(next == Theme.DARK ? "🌙" : "☀️");
        });

        soundButton.addActionListener(e -> {
            boolean enabled = !context.isSoundEnabled();
            context.setSoundEnabled(enabled);
            soundButton.setText(enabled ? "🔊" : "🔇");
        });

        ...
        fileButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            if (chooser.showOpenDialog(mainFrame) == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                // пока только уведомляем UI через EventBus (отправку подключим к коннектору из context)
                context.getEventBus().publish(
                        new FileTransferEvent(FileTransferEvent.Type.STARTED,
                                file.getName(), 0, file.length(), true, null));
                JOptionPane.showMessageDialog(mainFrame,
                        "Отправка файла будет подключена после проброса ObjectOutputStream в ApplicationContext.");
            }
        });
        ...
    }
    ...
}
