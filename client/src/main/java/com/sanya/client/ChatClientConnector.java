package com.sanya.client;

import com.sanya.Message;
import com.sanya.events.*;
import com.sanya.files.FileChunk;
import com.sanya.files.FileTransferRequest;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Сетевой коннектор для обмена данными с сервером чата.
 * Отвечает только за сетевую коммуникацию, без бизнес-логики подписок.
 */
public class ChatClientConnector {

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

        // УДАЛЕНО: Подписка на MessageSendEvent - теперь это в EventSubscriptionsManager
        // eventBus.subscribe(MessageSendEvent.class, e -> safeSendMessage(e.text()));

        System.out.println("[ChatClientConnector] Connector created for user: " + username);
    }

    /**
     * Подключение к серверу
     */
    public void connect() {
        if (connecting.get() || connected.get()) {
            System.out.println("[ChatClientConnector] Already connected or connecting");
            return;
        }

        connecting.set(true);

        try {
            System.out.println("[ChatClientConnector] Connecting to " + host + ":" + port);

            socket = new Socket(host, port);
            socket.setSoTimeout(30000); // 30 секунд таймаут

            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in  = new ObjectInputStream(socket.getInputStream());

            connected.set(true);
            connecting.set(false);

            // Отправляем приветственное сообщение
            out.writeObject(new Message(username, "<<<HELLO>>>"));
            out.flush();

            // Запускаем поток чтения
            readerThread = new Thread(this::listenLoop, "ChatClientReader-" + username);
            readerThread.setDaemon(true);
            readerThread.start();

            System.out.println("[ChatClientConnector] Successfully connected to server");

        } catch (IOException e) {
            connecting.set(false);
            connected.set(false);

            String errorMsg = "Connection failed: " + e.getMessage();
            System.err.println("[ChatClientConnector] " + errorMsg);

            eventBus.publish(new MessageReceivedEvent(
                    new Message("SYSTEM", errorMsg, Message.Type.SYSTEM)
            ));
            close();
        } catch (Exception e) {
            connecting.set(false);
            connected.set(false);

            String errorMsg = "Unexpected connection error: " + e.getMessage();
            System.err.println("[ChatClientConnector] " + errorMsg);

            eventBus.publish(new MessageReceivedEvent(
                    new Message("SYSTEM", errorMsg, Message.Type.SYSTEM)
            ));
            close();
        }
    }

    /**
     * Основной цикл чтения данных от сервера
     */
    private void listenLoop() {
        System.out.println("[ChatClientConnector] Starting listen loop");

        try {
            while (connected.get() && !socket.isClosed()) {
                Object obj = in.readObject();
                processIncomingObject(obj);
            }
        } catch (EOFException e) {
            System.out.println("[ChatClientConnector] Server closed connection (EOF)");
        } catch (StreamCorruptedException e) {
            System.out.println("[ChatClientConnector] Stream corrupted, disconnecting");
        } catch (IOException e) {
            if (connected.get()) {
                System.err.println("[ChatClientConnector] Read error: " + e.getMessage());
            }
        } catch (ClassNotFoundException e) {
            System.err.println("[ChatClientConnector] Unknown object received: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("[ChatClientConnector] Unexpected error in listen loop: " + e.getMessage());
            e.printStackTrace();
        } finally {
            System.out.println("[ChatClientConnector] Listen loop ended");
            handleDisconnection();
        }
    }

    /**
     * Обработка входящих объектов от сервера
     */
    private void processIncomingObject(Object obj) {
        try {
            if (obj instanceof Message msg) {
                eventBus.publish(new MessageReceivedEvent(msg));

            } else if (obj instanceof UserListUpdatedEvent list) {
                eventBus.publish(list);

            } else if (obj instanceof FileTransferRequest req) {
                eventBus.publish(new FileIncomingEvent(req, in));

            } else if (obj instanceof FileChunk chunk) {
                if (!"voice".equals(chunk.getFilename())) {
                    eventBus.publish(new FileChunkEvent(chunk));
                }

            } else if (obj instanceof VoiceMessageReadyEvent evt) {
                // ВАЖНО: НЕ фильтруем по username - сервер уже это сделал
                // EventSubscriptionsManager позаботится о фильтрации если нужно
                eventBus.publish(evt);

            } else {
                System.out.println("[ChatClientConnector] Unknown object type: " + obj.getClass().getSimpleName());
            }

        } catch (Exception e) {
            System.err.println("[ChatClientConnector] Error processing incoming object: " + e.getMessage());
            eventBus.publish(new SystemMessageEvent("Error processing server data: " + e.getMessage()));
        }
    }

    /**
     * Безопасная отправка текстового сообщения
     * Теперь public для доступа из EventSubscriptionsManager
     */
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
            String errorMsg = "Send failed: " + e.getMessage();
            System.err.println("[ChatClientConnector] " + errorMsg);

            eventBus.publish(new MessageReceivedEvent(
                    new Message("SYSTEM", errorMsg, Message.Type.SYSTEM)
            ));

            // При ошибке отправки считаем что соединение разорвано
            handleDisconnection();
        }
    }

    /**
     * Безопасная отправка любого объекта
     */
    public void safeSendObject(Object obj) {
        if (!connected.get() || out == null) {
            System.err.println("[ChatClientConnector] Cannot send object - not connected");
            return;
        }

        try {
            synchronized (out) {
                out.writeObject(obj);
                out.flush();
            }
        } catch (IOException e) {
            System.err.println("[ChatClientConnector] Failed to send object: " + e.getMessage());
            handleDisconnection();
        }
    }

    /**
     * Обработка разрыва соединения
     */
    private void handleDisconnection() {
        if (connected.compareAndSet(true, false)) {
            System.out.println("[ChatClientConnector] Handling disconnection");

            eventBus.publish(new UserDisconnectedEvent(username));
            eventBus.publish(new MessageReceivedEvent(
                    new Message("SYSTEM", "Disconnected from server", Message.Type.SYSTEM)
            ));

            close();
        }
    }

    /**
     * Получение выходного потока (для отправки файлов и голосовых сообщений)
     */
    public ObjectOutputStream getOutputStream() {
        return out;
    }

    /**
     * Проверка подключения
     */
    public boolean isConnected() {
        return connected.get() && socket != null && !socket.isClosed();
    }

    /**
     * Закрытие соединения и очистка ресурсов
     */
    public void close() {
        System.out.println("[ChatClientConnector] Closing connection");

        connected.set(false);
        connecting.set(false);

        // Останавливаем поток чтения
        if (readerThread != null && readerThread.isAlive()) {
            readerThread.interrupt();
        }

        // Закрываем потоки
        if (out != null) {
            try {
                out.close();
            } catch (IOException ignored) {}
            out = null;
        }

        if (in != null) {
            try {
                in.close();
            } catch (IOException ignored) {}
            in = null;
        }

        // Закрываем сокет
        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException ignored) {}
            socket = null;
        }

        System.out.println("[ChatClientConnector] Connection closed");
    }

    /**
     * Деструктор для дополнительной безопасности
     */
    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }
}