package com.sanya.client.net;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ChatTransport — низкоуровневый TCP-транспорт для обмена сериализованными объектами.
 * Не содержит бизнес-логики, работает только с сетевым вводом-выводом.
 *
 * Назначение:
 *  - Управляет TCP-соединением (подключение, чтение, отправка, закрытие).
 *  - Вызывает колбэки слушателя {@link TransportListener} при получении сообщений или ошибках.
 *  - Используется классами верхнего уровня, например {@link ChatConnector}.
 *
 * Потокобезопасность:
 *  - Отправка синхронизирована по потоку {@code out}.
 *  - Состояние соединения защищено {@link AtomicBoolean connected}.
 */
public class ChatTransport implements AutoCloseable {

    private static final Logger log = Logger.getLogger(ChatTransport.class.getName());

    /** Адрес сервера */
    private final String host;

    /** Порт сервера */
    private final int port;

    /** TCP-сокет */
    private Socket socket;

    /** Потоки ввода и вывода объектов */
    private ObjectInputStream in;
    private ObjectOutputStream out;

    /** Фоновый поток чтения */
    private Thread readerThread;

    /** Флаг состояния соединения */
    private final AtomicBoolean connected = new AtomicBoolean(false);

    /** Обработчик событий транспорта */
    private TransportListener listener;

    /**
     * Создаёт новый транспортный канал.
     *
     * @param host хост сервера
     * @param port порт сервера
     */
    public ChatTransport(String host, int port) {
        this.host = host;
        this.port = port;
        log.config("ChatTransport created for " + host + ":" + port);
    }

    /** Назначает слушателя событий транспорта. */
    public void setListener(TransportListener listener) {
        this.listener = listener;
    }

    /**
     * Подключается к серверу и запускает поток чтения.
     *
     * @throws IOException если не удалось установить соединение
     */
    public synchronized void connect() throws IOException {
        if (connected.get()) {
            log.config("Already connected, skipping connect()");
            return;
        }

        log.config("Connecting to " + host + ":" + port);
        socket = new Socket(host, port);
        socket.setSoTimeout(0);

        out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();
        in = new ObjectInputStream(socket.getInputStream());

        connected.set(true);
        log.config("Socket connected to " + socket.getRemoteSocketAddress());

        readerThread = new Thread(this::listenLoop, "ChatTransport-Reader");
        readerThread.setDaemon(true);
        readerThread.start();

        log.info("Connected to " + host + ":" + port);
    }

    /**
     * Основной цикл чтения объектов из входящего потока.
     * Вызывает слушатель для каждого полученного объекта.
     */
    private void listenLoop() {
        try {
            while (connected.get() && !socket.isClosed()) {
                Object obj = in.readObject();
                if (listener != null) listener.onMessage(obj);
            }
        } catch (EOFException | SocketException e) {
            log.info("Disconnected: " + e.getMessage());
            if (listener != null) listener.onDisconnect(e);
        } catch (Exception e) {
            log.log(Level.SEVERE, "Read error", e);
            if (listener != null) listener.onDisconnect(e);
        } finally {
            close();
        }
    }

    /**
     * Отправляет сериализованный объект на сервер.
     *
     * @param obj объект для отправки
     * @throws IOException если соединение разорвано или произошла ошибка записи
     */
    public synchronized void send(Object obj) throws IOException {
        if (!connected.get() || out == null) {
            throw new IOException("Transport not connected");
        }
        synchronized (out) {
            out.writeObject(obj);
            out.flush();
            log.fine("Sent object: " + obj.getClass().getSimpleName());
        }
    }

    /** Проверяет активность соединения. */
    public boolean isConnected() {
        return connected.get();
    }

    /**
     * Безопасно закрывает все ресурсы.
     * Соединение считается завершённым, listener уведомляется об отключении.
     */
    @Override
    public synchronized void close() {
        if (!connected.getAndSet(false)) return;

        log.config("Closing ChatTransport for " + host + ":" + port);
        try { if (in != null) in.close(); } catch (IOException ignored) {}
        try { if (out != null) out.close(); } catch (IOException ignored) {}
        try { if (socket != null && !socket.isClosed()) socket.close(); } catch (IOException ignored) {}

        in = null;
        out = null;
        socket = null;

        if (listener != null) listener.onDisconnect(null);
        log.config("ChatTransport closed cleanly");
    }

    /**
     * Интерфейс слушателя транспорта.
     * Используется для уведомления верхнего уровня о событиях приёма и отключения.
     */
    public interface TransportListener {
        /** Вызывается при получении нового объекта. */
        void onMessage(Object obj);

        /** Вызывается при отключении или ошибке. */
        void onDisconnect(Exception cause);
    }
}
