package com.sanya.client.net;

import com.sanya.Message;
import com.sanya.client.ApplicationContext;
import com.sanya.crypto.*;
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

import javax.crypto.SecretKey;
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
        if (text == null || text.isBlank()) return;

        try {
            String self = username;
            Map<String, SignedPreKeyBundle> bundles = ctx.getKnownBundles();

            // Если нет известных ключей — отправляем в открытом виде
            if (bundles.isEmpty()) {
                transport.send(new Message(self, text));
                return;
            }

            // Для каждого известного получателя
            for (String peer : bundles.keySet()) {
                if (peer.equals(self)) continue;
                try {
                    // === Вычисляем общий секрет ===
                    var bundle = ctx.getKnownBundles().get(peer);
                    var peerPub = KeyUtils.x25519FromRaw(bundle.getX25519Public());
                    byte[] shared = KeyUtils.sharedSecret(ctx.getX25519KeyPair().getPrivate(), peerPub);
                    byte[] aesKeyBytes = HKDF.deriveAesKey(shared, null, "Sanya-Chat-AES");
                    SecretKey aesKey = new javax.crypto.spec.SecretKeySpec(aesKeyBytes, "AES");

                    // === Шифруем текст ===
                    byte[] aad = Bytes.concat(Bytes.utf8(self), Bytes.utf8(peer));
                    var box = AesGcm.encrypt(aesKey, Bytes.utf8(text), aad);

                    // === Упаковываем в EncryptedPayload ===
                    EncryptedPayload payload = new EncryptedPayload(box.iv, box.ct, aad);

                    // === Заворачиваем в Message ===
                    Message msg = new Message(self, "[encrypted]");
                    msg.setAttachment(payload);

                    transport.send(msg);
                    log.fine("Encrypted message sent to " + peer);
                } catch (Exception e) {
                    log.log(Level.WARNING, "Encryption failed for " + peer, e);
                }
            }
        } catch (IOException e) {
            log.log(Level.WARNING, "Send failed", e);
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
    // === Вспомогательная логика шифрования/дешифрования ===
    private byte[] deriveSharedKey(String peer) throws Exception {
        var bundle = ctx.getKnownBundles().get(peer);
        if (bundle == null) return null;

        PublicKey peerPub = KeyUtils.x25519FromRaw(bundle.getX25519Public());
        byte[] shared = KeyUtils.sharedSecret(ctx.getX25519KeyPair().getPrivate(), peerPub);
        return HKDF.deriveAesKey(shared, null, "Sanya-Chat-AES");
    }

    private String decryptMessage(String sender, byte[] iv, byte[] ciphertext, byte[] aad) throws Exception {
        byte[] keyBytes = deriveSharedKey(sender);
        if (keyBytes == null) return "[UNVERIFIED USER]";
        javax.crypto.SecretKey key = new javax.crypto.spec.SecretKeySpec(keyBytes, "AES");
        byte[] plain = AesGcm.decrypt(key, iv, ciphertext, aad);
        return new String(plain, java.nio.charset.StandardCharsets.UTF_8);
    }
    // === Вспомогательная структура для передачи шифрованных блоков ===
    private static final class EncryptedPayload implements java.io.Serializable {
        final byte[] iv;
        final byte[] ct;
        final byte[] aad;
        EncryptedPayload(byte[] iv, byte[] ct, byte[] aad) {
            this.iv = iv; this.ct = ct; this.aad = aad;
        }
    }

}
