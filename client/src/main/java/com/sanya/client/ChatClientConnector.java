package com.sanya.client;

import com.sanya.Message;
import com.sanya.events.*;
import com.sanya.files.FileChunk;
import com.sanya.files.FileTransferRequest;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Сетевой коннектор для обмена данными с сервером чата.
 * Отвечает только за сетевую коммуникацию, без бизнес-логики подписок.
 */
public class ChatClientConnector implements AutoCloseable {

    private static final Logger log = Logger.getLogger(ChatClientConnector.class.getName());

    private final String host;
    private final int port;
    private final String username;
    private final EventBus eventBus;

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Thread readerThread;
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicBoolean connecting = new AtomicBoolean(false);

    public ChatClientConnector(String host, int port, String username, EventBus eventBus) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.eventBus = eventBus;
        log.info(() -> "[ChatClientConnector] Connector created for user: " + username);
    }

    /** Подключение к серверу */
    public void connect() {
        if (connecting.get() || connected.get()) {
            log.warning("[ChatClientConnector] Already connected or connecting");
            return;
        }

        connecting.set(true);

        try {
            log.info(() -> "[ChatClientConnector] Connecting to " + host + ":" + port);
            socket = new Socket(host, port);
            socket.setSoTimeout(30000);

            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            connected.set(true);
            connecting.set(false);

            out.writeObject(new Message(username, "<<<HELLO>>>"));
            out.flush();

            readerThread = new Thread(this::listenLoop, "ChatClientReader-" + username);
            readerThread.setDaemon(true);
            readerThread.start();

            log.info("[ChatClientConnector] Successfully connected to server");

        } catch (IOException e) {
            handleConnectionError("Connection failed", e);
        } catch (Exception e) {
            handleConnectionError("Unexpected connection error", e);
        }
    }

    private void handleConnectionError(String prefix, Exception e) {
        connecting.set(false);
        connected.set(false);
        String errorMsg = prefix + ": " + e.getMessage();
        log.log(Level.SEVERE, "[ChatClientConnector] " + errorMsg, e);
        eventBus.publish(new MessageReceivedEvent(
                new Message("SYSTEM", errorMsg, Message.Type.SYSTEM)
        ));
        close();
    }

    /** Основной цикл чтения данных от сервера */
    private void listenLoop() {
        log.info("[ChatClientConnector] Starting listen loop");
        try {
            while (connected.get() && !socket.isClosed()) {
                Object obj = in.readObject();
                processIncomingObject(obj);
            }
        } catch (EOFException e) {
            log.info("[ChatClientConnector] Server closed connection (EOF)");
        } catch (StreamCorruptedException e) {
            log.warning("[ChatClientConnector] Stream corrupted, disconnecting");
        } catch (IOException e) {
            if (connected.get()) log.log(Level.WARNING, "[ChatClientConnector] Read error", e);
        } catch (ClassNotFoundException e) {
            log.log(Level.SEVERE, "[ChatClientConnector] Unknown object received", e);
        } catch (Exception e) {
            log.log(Level.SEVERE, "[ChatClientConnector] Unexpected error in listen loop", e);
        } finally {
            log.info("[ChatClientConnector] Listen loop ended");
            handleDisconnection();
        }
    }

    /** Обработка входящих объектов от сервера */
    private void processIncomingObject(Object obj) {
        try {
            if (obj instanceof Message msg) {
                eventBus.publish(new MessageReceivedEvent(msg));
            } else if (obj instanceof UserListUpdatedEvent list) {
                eventBus.publish(list);
            } else if (obj instanceof FileTransferRequest req) {
                eventBus.publish(new FileIncomingEvent(req, in));
            } else if (obj instanceof FileChunk chunk) {
                if (!"voice".equals(chunk.getFilename()))
                    eventBus.publish(new FileChunkEvent(chunk));
            } else if (obj instanceof VoiceMessageReadyEvent evt) {
                eventBus.publish(evt);
            } else {
                log.warning(() -> "[ChatClientConnector] Unknown object type: " + obj.getClass().getSimpleName());
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "[ChatClientConnector] Error processing incoming object", e);
            eventBus.publish(new SystemMessageEvent("Error processing server data: " + e.getMessage()));
        }
    }

    /** Безопасная отправка текстового сообщения */
    public void safeSendMessage(String text) {
        if (!connected.get() || out == null) {
            eventBus.publish(new MessageReceivedEvent(
                    new Message("SYSTEM", "Not connected to server", Message.Type.SYSTEM)
            ));
            return;
        }
        try {
            synchronized (out) {
                out.writeObject(new Message(username, text));
                out.flush();
            }
        } catch (IOException e) {
            log.log(Level.WARNING, "[ChatClientConnector] Send failed", e);
            eventBus.publish(new MessageReceivedEvent(
                    new Message("SYSTEM", "Send failed: " + e.getMessage(), Message.Type.SYSTEM)
            ));
            handleDisconnection();
        }
    }

    /** Безопасная отправка любого объекта */
    public void safeSendObject(Object obj) {
        if (!connected.get() || out == null) {
            log.warning("[ChatClientConnector] Cannot send object - not connected");
            return;
        }
        try {
            synchronized (out) {
                out.writeObject(obj);
                out.flush();
            }
        } catch (IOException e) {
            log.log(Level.WARNING, "[ChatClientConnector] Failed to send object", e);
            handleDisconnection();
        }
    }

    /** Обработка разрыва соединения */
    private void handleDisconnection() {
        if (connected.compareAndSet(true, false)) {
            log.info("[ChatClientConnector] Handling disconnection");
            eventBus.publish(new UserDisconnectedEvent(username));
            eventBus.publish(new MessageReceivedEvent(
                    new Message("SYSTEM", "Disconnected from server", Message.Type.SYSTEM)
            ));
            close();
        }
    }

    /** Получение выходного потока (для отправки файлов и голосовых сообщений) */
    public ObjectOutputStream getOutputStream() {
        return out;
    }

    /** Проверка подключения */
    public boolean isConnected() {
        return connected.get() && socket != null && !socket.isClosed();
    }

    /** Закрытие соединения и очистка ресурсов */
    @Override
    public void close() {
        log.info("[ChatClientConnector] Closing connection");
        connected.set(false);
        connecting.set(false);

        if (readerThread != null && readerThread.isAlive())
            readerThread.interrupt();

        try {
            if (out != null) out.close();
        } catch (IOException ignored) {}
        try {
            if (in != null) in.close();
        } catch (IOException ignored) {}
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {}

        out = null;
        in = null;
        socket = null;

        log.info("[ChatClientConnector] Connection closed");
    }
}
