package com.sanya.client.net;

import com.sanya.Message;
import com.sanya.client.ApplicationContext;
import com.sanya.crypto.Bytes;
import com.sanya.crypto.KeyUtils;
import com.sanya.crypto.SignatureUtils;
import com.sanya.crypto.SignedPreKeyBundle;
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
import java.security.KeyFactory;
import java.security.PublicKey;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ChatConnector — уровень интеграции между EventBus и ChatTransport.
 * Обрабатывает сетевые события, публикует их в EventBus, отправляет исходящие.
 */
public class ChatConnector implements ChatTransport.TransportListener, AutoCloseable {

    private static final Logger log = Logger.getLogger(ChatConnector.class.getName());
    private final ApplicationContext ctx;
    private final ChatTransport transport;
    private final EventBus bus;
    private final String username;
    private final AtomicBoolean reconnecting = new AtomicBoolean(false);
    private volatile boolean manualClose = false;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public ChatConnector(ApplicationContext ctx, String host, int port, String username, EventBus bus) {
        this.ctx = ctx;
        this.transport = new ChatTransport(host, port);
        this.transport.setListener(this);
        this.bus = bus;
        this.username = username;
        log.config("Initializing ChatConnector for user: " + username + " (" + host + ":" + port + ")");
    }

    /** Подключение к серверу. */
    public void connect() {
        log.config("Attempting to connect via ChatTransport...");
        try {
            transport.connect();
            transport.send(new Message(username, "<<<HELLO>>>"));

            // === crypto handshake ===
            byte[] xpub = KeyUtils.x25519Raw((java.security.interfaces.XECPublicKey)
                    ctx.getX25519KeyPair().getPublic());
            byte[] toSign = Bytes.concat(Bytes.utf8(username), xpub);
            byte[] sig = SignatureUtils.signEd25519(
                    ctx.getEd25519KeyPair().getPrivate(), toSign);

            SignedPreKeyBundle bundle = new SignedPreKeyBundle(
                    username,
                    xpub,
                    ctx.getEd25519KeyPair().getPublic().getEncoded(),
                    sig,
                    System.currentTimeMillis()
            );

            transport.send(bundle);
            log.info("Sent SignedPreKeyBundle for " + username);

            bus.publish(new SystemInfoEvent("Connected to server"));
        } catch (IOException e) {
            handleSendError(e);
        } catch (Exception e) {
            log.log(Level.SEVERE, "Crypto handshake failed", e);
        }
    }

    /** Отправка текстового сообщения. */
    public void sendMessage(String text) {
        try {
            transport.send(new Message(username, text));
        } catch (IOException e) {
            handleSendError(e);
        }
    }

    /** Отправка произвольного объекта (файл, голос и т.п.). */
    public void sendObject(Object obj) {
        try {
            transport.send(obj);
        } catch (IOException e) {
            handleSendError(e);
        }
    }

    /** Обработка входящего объекта. */
    @Override
    public void onMessage(Object obj) {
        if (obj instanceof SignedPreKeyBundle bundle) {
            try {
                PublicKey signPub = KeyFactory.getInstance("Ed25519")
                        .generatePublic(new java.security.spec.X509EncodedKeySpec(bundle.getEd25519Public()));
                boolean ok = SignatureUtils.verifyEd25519(
                        signPub,
                        Bytes.concat(Bytes.utf8(bundle.getUsername()), bundle.getX25519Public()),
                        bundle.getSignature()
                );
                if (!ok) {
                    log.warning("Signature invalid for bundle from " + bundle.getUsername());
                    return;
                }
                ctx.getKnownBundles().put(bundle.getUsername(), bundle);
                log.info("Stored verified bundle from " + bundle.getUsername());
            } catch (Exception e) {
                log.log(Level.WARNING, "Failed to process SignedPreKeyBundle", e);
            }
            return;
        }

        if (obj instanceof Map<?, ?> map && !map.isEmpty()) {
            Object any = map.values().iterator().next();
            if (any instanceof SignedPreKeyBundle) {
                @SuppressWarnings("unchecked")
                Map<String, SignedPreKeyBundle> bundles = (Map<String, SignedPreKeyBundle>) map;
                ctx.getKnownBundles().putAll(bundles);
                log.info("Received bundle map size=" + bundles.size());
            }
        }
    }

    /** Потеря соединения. */
    @Override
    public void onDisconnect(Exception cause) {
        if (reconnecting.get()) return;

        String reason = cause != null ? cause.getMessage() : "Connection closed";
        bus.publish(new ConnectionLostEvent(reason, true));
        log.info("Disconnected: " + reason);

        if (manualClose) {
            log.config("Manual close detected — skipping reconnect");
            return;
        }

        scheduleReconnect(1);
    }

    /** Авто-реконнект с экспоненциальным бэкофом. */
    private void scheduleReconnect(int attempt) {
        if (reconnecting.getAndSet(true)) return;
        int delay = Math.min(30, (int) Math.pow(2, attempt));
        log.config("Scheduling reconnect attempt " + attempt + " after " + delay + "s");

        scheduler.schedule(() -> {
            try {
                connect();
                reconnecting.set(false);
                bus.publish(new SystemInfoEvent("Reconnected"));
                log.config("Reconnection succeeded on attempt " + attempt);
            } catch (Exception e) {
                log.log(Level.WARNING, "Reconnect attempt failed", e);
                scheduleReconnect(attempt + 1);
            }
        }, delay, java.util.concurrent.TimeUnit.SECONDS);
    }

    private void handleSendError(IOException e) {
        log.warning("Send failed: " + e.getMessage());
        bus.publish(new SystemMessageEvent("Send failed: " + e.getMessage()));
        onDisconnect(e);
    }

    public boolean isConnected() {
        return transport.isConnected();
    }

    @Override
    public void close() {
        manualClose = true;
        transport.close();
        scheduler.shutdownNow();
    }
}
