package com.sanya.client.ui;

import com.sanya.client.ApplicationContext;
import com.sanya.client.ChatClientController;
import com.sanya.client.core.AppCore;
import com.sanya.client.facade.UIFacade;
import com.sanya.client.ui.main.ChatMainPanel;
import com.sanya.client.facade.swing.SwingUIFacade;

import javax.swing.*;

/**
 * Главный UI-контейнер клиента.
 * Содержит ChatMainPanel и связывает UI с ApplicationContext через UIFacade.
 */
public class ChatClientUI extends JFrame {

    private final ApplicationContext ctx;
    private final AppCore core;
    private final ChatMainPanel mainPanel;

    public ChatClientUI(ApplicationContext ctx) {
        this.ctx = ctx;
        this.core = ctx.core();

        setTitle("Sanya Chat — " + ctx.getUserSettings().getName());
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(800, 550);
        setLocationRelativeTo(null);

        // основной панельный UI
        mainPanel = new ChatMainPanel(ctx);
        setContentPane(mainPanel);

        // связываем UIFacade с контекстом
        UIFacade facade = new SwingUIFacade(ctx, mainPanel);
        ctx.setUIFacade(facade);

        // создаём контроллер
        new ChatClientController(ctx);

        // применяем текущую тему
        SwingUtilities.invokeLater(() ->
                mainPanel.applyTheme(ctx.getUiSettings().getTheme()));
    }
    public ChatMainPanel getMainPanel() {
        return mainPanel;
    }
    public void showUI() {
        SwingUtilities.invokeLater(() -> setVisible(true));
    }
}
