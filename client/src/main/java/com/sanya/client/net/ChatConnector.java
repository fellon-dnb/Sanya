package com.sanya.client.net;

import com.sanya.Message;
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

    private final ChatTransport transport;
    private final EventBus bus;
    private final String username;
    private final AtomicBoolean reconnecting = new AtomicBoolean(false);
    private volatile boolean manualClose = false;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public ChatConnector(String host, int port, String username, EventBus bus) {
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
            bus.publish(new SystemInfoEvent("Connected to server"));
            log.config("ChatConnector connection established successfully");
        } catch (IOException e) {
            log.log(Level.WARNING, "Connect failed", e);
            bus.publish(new ConnectionLostEvent(e.getMessage(), true));
            scheduleReconnect(1);
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
        try {
            if (obj instanceof Message msg) {
                bus.publish(new MessageReceivedEvent(msg));
            } else if (obj instanceof UserListUpdatedEvent e) {
                bus.publish(e);
            } else if (obj instanceof FileTransferRequest e) {
                bus.publish(new FileIncomingEvent(e, null));
            } else if (obj instanceof FileChunk c) {
                bus.publish(new FileChunkEvent(c));
            } else if (obj instanceof VoiceMessageReadyEvent v) {
                bus.publish(v);
            } else {
                log.fine("Unknown incoming object: " + obj.getClass().getSimpleName());
            }
        } catch (Exception ex) {
            log.log(Level.WARNING, "Failed to process incoming object", ex);
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

        if (scheduler.isShutdown() || scheduler.isTerminated()) {
            log.warning("Scheduler already shut down, skipping reconnect");
            return;
        }

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
        log.config("Closing ChatConnector manually");
        transport.close();
        scheduler.shutdownNow();
    }
}
