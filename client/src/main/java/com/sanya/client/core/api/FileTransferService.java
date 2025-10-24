package com.sanya.client.core.api;

import com.sanya.files.FileChunk;

import java.io.File;
import java.util.function.Consumer;

/**
 * Интерфейс для сервиса передачи файлов.
 * Определяет базовый контракт для отправки и приёма файловых данных между клиентами.
 *
 * Назначение:
 *  - Разделить транспортную и прикладную логику передачи файлов.
 *  - Упростить замену реализации (локальная передача, через сервер, P2P и т.д.)
 *  - Обеспечить единый интерфейс для модулей, обрабатывающих FileChunk.
 *
 * Использование:
 *  Реализация может использовать EventBus и ChatConnector для пересылки блоков файлов,
 *  а также уведомлять о прогрессе через события.
 */
public interface FileTransferService {

    /**
     * Отправляет файл получателю по частям.
     *
     * @param recipient имя или идентификатор получателя
     * @param file      файл для отправки
     * @param sender    функция, выполняющая фактическую отправку объекта (например, через сеть)
     */
    void sendFile(String recipient, File file, Consumer<Object> sender);

    /**
     * Обрабатывает входящий фрагмент файла.
     *
     * @param chunk объект FileChunk, содержащий часть файла
     */
    void receiveFile(FileChunk chunk);
}
