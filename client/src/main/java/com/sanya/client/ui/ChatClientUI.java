package com.sanya.client.ui;

import com.sanya.client.ApplicationContext;
import com.sanya.client.ChatClientController;
import com.sanya.client.core.AppCore;
import com.sanya.client.facade.UIFacade;
import com.sanya.client.facade.swing.SwingUIFacade;
import com.sanya.client.ui.main.ChatMainPanel;

import javax.swing.*;

/**
 * ChatClientUI — главный контейнер графического интерфейса клиента.
 * Отвечает за инициализацию визуальной части приложения, создание контроллера
 * и связывание UI с {@link ApplicationContext} через {@link UIFacade}.
 *
 * Назначение:
 * - Содержит основную панель чата {@link ChatMainPanel}.
 * - Инициализирует {@link SwingUIFacade}, обеспечивающий связь между UI и логикой.
 * - Создаёт {@link ChatClientController}, управляющий поведением клиента.
 * - Автоматически применяет выбранную тему из настроек.
 *
 * Использование:
 * ChatClientUI ui = new ChatClientUI(ctx);
 * ui.showUI();
 */
public final class ChatClientUI extends JFrame {

    /** Контекст приложения. */
    private final ApplicationContext ctx;

    /** Ядро приложения (DI-контейнер и сервисы). */
    private final AppCore core;

    /** Основная панель пользовательского интерфейса. */
    private final ChatMainPanel mainPanel;

    /**
     * Конструктор инициализирует графический интерфейс клиента.
     *
     * @param ctx контекст приложения, обеспечивающий доступ к сервисам и настройкам
     */
    public ChatClientUI(ApplicationContext ctx) {
        this.ctx = ctx;
        this.core = ctx.core();

        setTitle("Sanya Chat — " + ctx.getUserSettings().getName());
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(800, 550);
        setLocationRelativeTo(null);

        // Создание основной панели UI
        mainPanel = new ChatMainPanel(ctx);
        setContentPane(mainPanel);

        // Устанавливаем фасад UI в контекст
        UIFacade facade = new SwingUIFacade(ctx, mainPanel);
        ctx.setUIFacade(facade);

        // Создание контроллера для обработки событий и сетевого взаимодействия
        new ChatClientController(ctx);

        // Применение текущей темы интерфейса
        SwingUtilities.invokeLater(() ->
                mainPanel.applyTheme(ctx.getUiSettings().getTheme()));
    }

    /** Возвращает основную панель пользовательского интерфейса. */
    public ChatMainPanel getMainPanel() {
        return mainPanel;
    }

    /** Отображает интерфейс пользователя. */
    public void showUI() {
        SwingUtilities.invokeLater(() -> setVisible(true));
    }
}
