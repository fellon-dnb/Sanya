package com.sanya.client.core;

import com.sanya.client.ApplicationContext;
import com.sanya.client.core.api.AudioSender;
import com.sanya.client.core.api.EventBus;
import com.sanya.client.core.api.FileTransferService;
import com.sanya.client.service.ChatService;
import com.sanya.client.service.audio.VoiceSender;
import com.sanya.client.service.audio.VoiceService;
import com.sanya.client.service.files.FileSender;
import com.sanya.client.ui.theme.ThemeManager;

/**
 * ServiceRegistry — централизованный реестр клиентских сервисов.
 * Обеспечивает единое место для инициализации и доступа к ключевым модулям приложения.
 *
 * Назначение:
 *  - Инкапсулирует создание всех сервисов и их зависимости.
 *  - Упрощает тестирование и масштабирование архитектуры.
 *  - Поддерживает согласованность через единый EventBus.
 *
 * Использование:
 *  Создаётся внутри {@link com.sanya.client.core.AppCore} и доступен через {@link ApplicationContext}.
 *  Пример: ctx.services().voice().startRecording();
 */
public final class ServiceRegistry {

    /** Сервис текстового чата (сообщения, очистка, отправка объектов) */
    private final ChatService chatService;

    /** Сервис записи и воспроизведения голосовых сообщений */
    private final VoiceService voiceService;

    /** Менеджер тем оформления */
    private final ThemeManager themeManager;

    /** Сервис передачи файлов */
    private final FileTransferService fileSender;

    /** Сервис отправки аудиопотоков */
    private final AudioSender voiceSender;

    /**
     * Конструктор реестра сервисов.
     *
     * @param ctx контекст приложения
     * @param bus клиентская шина событий
     */
    public ServiceRegistry(ApplicationContext ctx, EventBus bus) {
        this.chatService = new ChatService(bus);
        this.voiceService = new VoiceService(ctx);
        this.themeManager = new ThemeManager(ctx, bus);
        this.fileSender = new FileSender(bus);
        this.voiceSender = new VoiceSender(this.chatService);
    }

    /**
     * Возвращает сервис чата.
     */
    public ChatService chat() {
        return chatService;
    }

    /**
     * Возвращает сервис голосовых сообщений.
     */
    public VoiceService voice() {
        return voiceService;
    }

    /**
     * Возвращает менеджер тем оформления.
     */
    public ThemeManager theme() {
        return themeManager;
    }

    /**
     * Возвращает сервис передачи файлов.
     */
    public FileTransferService fileSender() {
        return fileSender;
    }

    /**
     * Возвращает отправитель аудиопотоков.
     */
    public AudioSender voiceSender() {
        return voiceSender;
    }
}
