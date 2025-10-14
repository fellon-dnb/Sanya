package com.sanya.client;

import com.sanya.client.commands.CommandHandler;
import com.sanya.client.di.DependencyContainer;
import com.sanya.client.service.ChatService;
import com.sanya.client.service.audio.VoiceService;
import com.sanya.client.settings.NetworkSettings;
import com.sanya.client.settings.UiSettings;
import com.sanya.client.settings.UserSettings;
import com.sanya.client.ui.UIFacade;
import com.sanya.events.EventBus;
import com.sanya.events.SimpleEventBus;

/**
 * Контекст приложения.
 * Объединяет все основные зависимости клиента (сервисный слой, UI, настройки)
 * и поддерживает DI-контейнер для удобной регистрации/извлечения компонентов.
 */
public final class ApplicationContext {

    private final DependencyContainer di = new DependencyContainer();

    // --- обычные зависимости (как у тебя) ---
    private final NetworkSettings networkSettings;
    private final UserSettings userSettings = new UserSettings();
    private final UiSettings uiSettings = new UiSettings();
    private final EventBus eventBus = new SimpleEventBus();
    private final CommandHandler commandHandler = new CommandHandler(this);
    private final Services services = new Services(this);
    private UIFacade uiFacade;

    //  конструктор
    public ApplicationContext(NetworkSettings networkSettings) {
        this.networkSettings = networkSettings;

        // Регистрация базовых зависимостей в DI
        di.registerSingleton(EventBus.class, () -> eventBus);
        di.registerSingleton(ChatService.class, () -> services.chat());
        di.registerSingleton(ApplicationContext.class, () -> this);
        di.registerSingleton(VoiceService.class, () -> new VoiceService(this));
    }

    //  доступ к DI
    public <T> T get(Class<T> type) {
        return di.get(type);
    }

    public DependencyContainer di() {
        return di;
    }

    //  стандартные геттеры
    public EventBus getEventBus() {
        return eventBus;
    }

    public NetworkSettings getNetworkSettings() {
        return networkSettings;
    }

    public UiSettings getUiSettings() {
        return uiSettings;
    }

    public UserSettings getUserSettings() {
        return userSettings;
    }

    public CommandHandler getCommandHandler() {
        return commandHandler;
    }

    public Services services() {
        return services;
    }

    //  вложенный класс для сервисов
    public static class Services {
        private final ChatService chatService;

        public Services(ApplicationContext ctx) {
            chatService = new ChatService(ctx.getEventBus());
        }

        public ChatService chat() {
            return chatService;
        }
    }

    //  фасад UI
    public UIFacade getUIFacade() {
        return uiFacade != null ? uiFacade : di.get(UIFacade.class);
    }

    public void setUIFacade(UIFacade uiFacade) {
        this.uiFacade = uiFacade;
        di.registerSingleton(UIFacade.class, () -> uiFacade);
    }
}
