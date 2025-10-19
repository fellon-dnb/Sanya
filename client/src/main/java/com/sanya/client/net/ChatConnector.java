package com.sanya.client.net;

import com.sanya.Message;
import com.sanya.client.ApplicationContext;
import com.sanya.client.security.Encryptor;
import com.sanya.client.security.KeyDirectory;
import com.sanya.crypto.Crypto;
import com.sanya.crypto.msg.EncryptedDirectMessage;
import com.sanya.crypto.msg.KeyDirectoryUpdate;
import com.sanya.crypto.msg.KeyHello;
import com.sanya.events.chat.MessageReceivedEvent;
import com.sanya.events.chat.UserListUpdatedEvent;
import com.sanya.events.core.EventBus;
import com.sanya.events.file.FileChunkEvent;
import com.sanya.events.file.FileIncomingEvent;
import com.sanya.events.system.ConnectionLostEvent;
import com.sanya.events.system.SystemInfoEvent;
import com.sanya.events.system.SystemMessageEvent;
import com.sanya.events.voice.VoiceMessageReadyEvent;
import com.sanya.files.FileChunk;
import com.sanya.files.FileTransferRequest;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ChatConnector — интеграция EventBus <-> ChatTransport.
 * Поддерживает reconnection, старый PreKey-протокол и новое E2EE (X25519 + AES-GCM).
 */
public final class ChatConnector implements ChatTransport.TransportListener, AutoCloseable {

    private static final Logger log = Logger.getLogger(ChatConnector.class.getName());

    private final ApplicationContext ctx;
    private final ChatTransport transport;
    private final EventBus bus;
    private final String username;

    // Реконнект
    private final AtomicBoolean reconnecting = new AtomicBoolean(false);
    private volatile boolean manualClose = false;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    // Новое E2EE
    private final KeyDirectory keyDir;
    private final Encryptor encryptor;

    public ChatConnector(ApplicationContext ctx,
                         String host, int port,
                         String username,
                         EventBus bus,
                         KeyDirectory keyDir,
                         Encryptor encryptor) {
        this.ctx = ctx;
        this.transport = new ChatTransport(host, port);
        this.transport.setListener(this);
        this.bus = bus;
        this.username = username;
        this.keyDir = keyDir;
        this.encryptor = encryptor;
        log.config("Initializing ChatConnector for user: " + username + " (" + host + ":" + port + ")");
    }

    /** === Подключение к серверу === */
    public void connect() {
        log.config("Connecting to server...");
        try {
            transport.connect();
            transport.send(new Message(username, "<<<HELLO>>>"));

            // --- Новый E2EE handshake ---
            String pubB64 = Crypto.encodePub(keyDir.myKeyPair().getPublic());
            transport.send(new KeyHello(username, pubB64));
            log.info("Sent X25519 KeyHello for " + username);

            bus.publish(new SystemInfoEvent("Connected to server"));
        } catch (IOException e) {
            handleSendError(e);
        } catch (Exception e) {
            log.log(Level.SEVERE, "Handshake failed", e);
        }
    }

    /** === Отправка простого текста (E2EE при наличии получателя) === */
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
                transport.send(dm);
                log.fine("Sent encrypted DM to " + to);
            } else {
                transport.send(new Message(username, text));
            }
        } catch (IOException e) {
            handleSendError(e);
        } catch (Exception e) {
            log.log(Level.WARNING, "Encryption failed", e);
        }
    }

    /** === Отправка произвольного объекта === */
    public void sendObject(Object obj) {
        try {
            transport.send(obj);
        } catch (IOException e) {
            handleSendError(e);
        }
    }

    /** === Обработка входящих сообщений === */
    @Override
    public void onMessage(Object obj) {
        try {
            // --- Обновление каталога ключей ---
            if (obj instanceof KeyDirectoryUpdate upd) {
                upd.userToX25519PubB64().forEach((u, b64) -> {
                    try { keyDir.putPub(u, Crypto.decodePub(b64)); } catch (Exception ignored) {}
                });
                log.info("Updated key directory: " + upd.userToX25519PubB64().size());
                return;
            }

            // --- Зашифрованные личные сообщения ---
            if (obj instanceof EncryptedDirectMessage dm) {
                byte[] plain = encryptor.decryptFrom(dm.from(), dm.nonce12(), dm.ciphertext());
                String text = new String(plain);
                bus.publish(new MessageReceivedEvent(new Message(dm.from(), "[private] " + text)));
                log.fine("Decrypted DM from " + dm.from());
                return;
            }

            // --- Обычные объекты чата ---
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

    /** === Потеря соединения и авто-реконнект === */
    @Override
    public void onDisconnect(Exception cause) {
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

    /** === Ошибка отправки === */
    private void handleSendError(IOException e) {
        log.warning("Send failed: " + e.getMessage());
        bus.publish(new SystemMessageEvent("Send failed: " + e.getMessage()));
        onDisconnect(e);
    }

    /** === Состояние соединения === */
    public boolean isConnected() {
        return transport.isConnected();
    }

    /** === Закрытие === */
    @Override
    public void close() {
        manualClose = true;
        transport.close();
        scheduler.shutdownNow();
    }
}
