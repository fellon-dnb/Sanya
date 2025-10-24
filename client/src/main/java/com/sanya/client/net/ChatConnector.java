package com.sanya.client.net;

import com.sanya.Message;
import com.sanya.client.ApplicationContext;
import com.sanya.client.core.api.Transport;
import com.sanya.client.security.Encryptor;
import com.sanya.client.security.KeyDirectory;
import com.sanya.crypto.Crypto;
import com.sanya.crypto.msg.EncryptedDirectMessage;
import com.sanya.crypto.msg.KeyDirectoryUpdate;
import com.sanya.crypto.msg.KeyHello;
import com.sanya.events.chat.MessageReceivedEvent;
import com.sanya.events.chat.UserListUpdatedEvent;
import com.sanya.events.core.DefaultEventBus;
import com.sanya.events.file.FileChunkEvent;
import com.sanya.events.file.FileIncomingEvent;
import com.sanya.events.system.ConnectionLostEvent;
import com.sanya.events.system.SystemInfoEvent;
import com.sanya.events.system.SystemMessageEvent;
import com.sanya.events.voice.VoiceMessageReadyEvent;
import com.sanya.files.FileChunk;
import com.sanya.files.FileTransferRequest;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ChatConnector — реализация сетевого транспорта и обработчик событий клиента.
 * Отвечает за соединение с сервером, обмен сообщениями, шифрование и реконнекты.
 *
 * Назначение:
 *  - Инкапсулировать сетевую логику TCP-взаимодействия.
 *  - Поддерживать E2EE (end-to-end encryption) через X25519 + AES-GCM.
 *  - Обеспечить стабильное соединение с автопереподключением.
 *
 * Использование:
 *  Создаётся в {@link ApplicationContext} и регистрируется как singleton.
 *  Работает через {@link DefaultEventBus}, публикуя и слушая события.
 */
public final class ChatConnector implements AutoCloseable, Transport {

    private static final Logger log = Logger.getLogger(ChatConnector.class.getName());

    /** Контекст приложения */
    private final ApplicationContext ctx;

    /** Шина событий для публикации системных и пользовательских событий */
    private final DefaultEventBus bus;

    /** Имя текущего пользователя */
    private final String username;

    /** TCP-соединение и потоки */
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    /** Флаги и планировщик для реконнекта */
    private final AtomicBoolean reconnecting = new AtomicBoolean(false);
    private volatile boolean manualClose = false;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    /** Криптографические зависимости */
    private final KeyDirectory keyDir;
    private final Encryptor encryptor;

    /** Параметры подключения */
    private final String host;
    private final int port;

    /**
     * Конструктор коннектора.
     *
     * @param ctx       контекст приложения
     * @param host      адрес сервера
     * @param port      порт сервера
     * @param username  имя пользователя
     * @param bus       локальный EventBus
     * @param keyDir    хранилище ключей
     * @param encryptor шифратор сообщений
     */
    public ChatConnector(ApplicationContext ctx,
                         String host, int port,
                         String username,
                         DefaultEventBus bus,
                         KeyDirectory keyDir,
                         Encryptor encryptor) {
        this.ctx = ctx;
        this.host = host;
        this.port = port;
        this.bus = bus;
        this.username = username;
        this.keyDir = keyDir;
        this.encryptor = encryptor;
        log.config("Initializing ChatConnector for user: " + username + " (" + host + ":" + port + ")");
    }

    /**
     * Устанавливает соединение с сервером и выполняет handshake.
     */
    @Override
    public void connect() {
        log.config("Connecting to server...");
        try {
            socket = new Socket(host, port);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            new Thread(this::listen, "ChatConnector-Listener").start();

            // Инициализационное сообщение
            send(new Message(username, "<<<HELLO>>>"));

            // Отправка публичного ключа (X25519)
            String pubB64 = Crypto.encodePub(keyDir.myKeyPair().getPublic());
            send(new KeyHello(username, pubB64));
            log.info("Sent X25519 KeyHello for " + username);

            bus.publish(new SystemInfoEvent("Connected to server"));
        } catch (IOException e) {
            handleSendError(e);
        } catch (Exception e) {
            log.log(Level.SEVERE, "Handshake failed", e);
        }
    }

    /**
     * Отправляет произвольный объект в сеть.
     *
     * @param message объект для отправки
     */
    @Override
    public void send(Object message) {
        try {
            synchronized (out) {
                out.writeObject(message);
                out.flush();
            }
        } catch (IOException e) {
            handleSendError(e);
        }
    }

    /**
     * Основной цикл приёма данных от сервера.
     * Обрабатывает все входящие объекты.
     */
    private void listen() {
        try {
            while (!socket.isClosed()) {
                Object obj = in.readObject();
                onMessage(obj);
            }
        } catch (IOException | ClassNotFoundException e) {
            if (!manualClose) onDisconnect(e);
        }
    }

    /**
     * Отправляет текстовое сообщение (с E2EE при наличии ключей).
     *
     * @param text сообщение пользователя
     */
    public void sendMessage(String text) {
        if (text == null || text.isBlank()) return;
        try {
            var peers = keyDir.allPubs().keySet().stream()
                    .filter(p -> !p.equals(username))
                    .toList();

            if (peers.size() == 1) {
                String to = peers.get(0);
                var enc = encryptor.encryptFor(to, text.getBytes());
                var dm = new EncryptedDirectMessage(username, to, enc.nonce(), enc.ct(),
                        "text/plain", null, null);
                send(dm);
                bus.publish(new MessageReceivedEvent(new Message(username, "[private] " + text)));
                log.fine("Sent encrypted DM to " + to);
            } else {
                send(new Message(username, text));
            }
        } catch (Exception e) {
            log.log(Level.WARNING, "Encryption failed", e);
        }
    }

    /**
     * Упрощённая отправка объектов.
     */
    public void sendObject(Object obj) {
        send(obj);
    }

    /**
     * Обработка входящих сообщений разных типов.
     */
    private void onMessage(Object obj) {
        try {
            if (obj instanceof KeyDirectoryUpdate upd) {
                upd.userToX25519PubB64().forEach((u, b64) -> {
                    try {
                        keyDir.putPub(u, Crypto.decodePub(b64));
                    } catch (Exception ignored) {}
                });
                log.info("Updated key directory: " + upd.userToX25519PubB64().size());
                return;
            }

            if (obj instanceof EncryptedDirectMessage dm) {
                byte[] plain = encryptor.decryptFrom(dm.from(), dm.nonce12(), dm.ciphertext());
                String text = new String(plain);
                bus.publish(new MessageReceivedEvent(new Message(dm.from(), "[private] " + text)));
                log.fine("Decrypted DM from " + dm.from());
                return;
            }

            if (obj instanceof Message message) {
                if (!"<<<HELLO>>>".equals(message.getText()))
                    bus.publish(new MessageReceivedEvent(message));
            } else if (obj instanceof FileTransferRequest req) {
                bus.publish(new FileIncomingEvent(req, null));
            } else if (obj instanceof FileChunk chunk) {
                bus.publish(new FileChunkEvent(chunk));
            } else if (obj instanceof VoiceMessageReadyEvent voiceMsg) {
                bus.publish(voiceMsg);
            } else if (obj instanceof SystemMessageEvent sysMsg) {
                bus.publish(sysMsg);
            } else if (obj instanceof SystemInfoEvent info) {
                bus.publish(info);
            } else if (obj instanceof UserListUpdatedEvent users) {
                bus.publish(users);
            }

        } catch (Exception e) {
            log.log(Level.WARNING, "Error processing message", e);
        }
    }

    /**
     * Обработка разрыва соединения и автопереподключение.
     */
    private void onDisconnect(Exception cause) {
        if (reconnecting.get()) return;

        String reason = cause != null ? cause.getMessage() : "Connection closed";
        bus.publish(new ConnectionLostEvent(reason, true));
        log.info("Disconnected: " + reason);

        if (manualClose) {
            log.config("Manual close — skip reconnect");
            return;
        }

        scheduleReconnect(1);
    }

    /**
     * Планировщик реконнекта с экспоненциальной задержкой.
     */
    private void scheduleReconnect(int attempt) {
        if (reconnecting.getAndSet(true)) return;
        int delay = Math.min(30, (int) Math.pow(2, attempt));
        log.config("Reconnect attempt " + attempt + " after " + delay + "s");

        scheduler.schedule(() -> {
            try {
                connect();
                reconnecting.set(false);
                bus.publish(new SystemInfoEvent("Reconnected"));
                log.info("Reconnection succeeded");
            } catch (Exception e) {
                log.log(Level.WARNING, "Reconnect failed", e);
                scheduleReconnect(attempt + 1);
            }
        }, delay, java.util.concurrent.TimeUnit.SECONDS);
    }

    /**
     * Обработка ошибок отправки.
     */
    private void handleSendError(IOException e) {
        log.warning("Send failed: " + e.getMessage());
        bus.publish(new SystemMessageEvent("Send failed: " + e.getMessage()));
        onDisconnect(e);
    }

    /**
     * Проверяет состояние подключения.
     *
     * @return true, если соединение активно
     */
    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    /**
     * Закрывает соединение и завершает планировщик реконнекта.
     */
    @Override
    public void close() {
        manualClose = true;
        try {
            if (socket != null) socket.close();
        } catch (IOException ignore) {}
        scheduler.shutdownNow();
    }
}
