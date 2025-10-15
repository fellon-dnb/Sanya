package com.sanya.client.net;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ChatTransport — низкоуровневый TCP транспорт без бизнес-логики.
 * Отвечает за подключение, чтение и отправку объектов.
 */
public class ChatTransport implements AutoCloseable {

    private static final Logger log = Logger.getLogger(ChatTransport.class.getName());

    private final String host;
    private final int port;

    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private Thread readerThread;
    private final AtomicBoolean connected = new AtomicBoolean(false);

    private TransportListener listener;

    public ChatTransport(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /** Установка слушателя событий (обработка входящих данных и ошибок). */
    public void setListener(TransportListener listener) {
        this.listener = listener;
    }

    /** Подключение к серверу. */
    public synchronized void connect() throws IOException {
        if (connected.get()) return;

        socket = new Socket(host, port);
        socket.setSoTimeout(0);

        out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();
        in = new ObjectInputStream(socket.getInputStream());

        connected.set(true);

        readerThread = new Thread(this::listenLoop, "ChatTransport-Reader");
        readerThread.setDaemon(true);
        readerThread.start();

        log.info(() -> "[ChatTransport] Connected to " + host + ":" + port);
    }

    /** Основной цикл чтения данных. */
    private void listenLoop() {
        try {
            while (connected.get() && !socket.isClosed()) {
                Object obj = in.readObject();
                if (listener != null) listener.onMessage(obj);
            }
        } catch (EOFException | SocketException e) {
            log.warning("[ChatTransport] Disconnected: " + e.getMessage());
            if (listener != null) listener.onDisconnect(e);
        } catch (Exception e) {
            log.log(Level.SEVERE, "[ChatTransport] Read error", e);
            if (listener != null) listener.onDisconnect(e);
        } finally {
            close();
        }
    }

    /** Отправка объекта на сервер. */
    public synchronized void send(Object obj) throws IOException {
        if (!connected.get() || out == null) {
            throw new IOException("Transport not connected");
        }
        synchronized (out) {
            out.writeObject(obj);
            out.flush();
        }
    }

    /** Проверка состояния. */
    public boolean isConnected() {
        return connected.get();
    }

    /** Безопасное закрытие. */
    @Override
    public synchronized void close() {
        if (!connected.getAndSet(false)) return;

        log.info("[ChatTransport] Closing transport");
        try { if (in != null) in.close(); } catch (IOException ignored) {}
        try { if (out != null) out.close(); } catch (IOException ignored) {}
        try { if (socket != null && !socket.isClosed()) socket.close(); } catch (IOException ignored) {}

        in = null;
        out = null;
        socket = null;

        if (listener != null) listener.onDisconnect(null);
    }

    /** Интерфейс слушателя для обратных вызовов. */
    public interface TransportListener {
        void onMessage(Object obj);
        void onDisconnect(Exception cause);
    }
}
