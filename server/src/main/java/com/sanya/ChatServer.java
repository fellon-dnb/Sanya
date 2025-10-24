package com.sanya;

import com.sanya.events.chat.UserListUpdatedEvent;

import com.sanya.files.FileChunk;
import com.sanya.files.FileTransferRequest;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import com.sanya.crypto.SignedPreKeyBundle;
import com.sanya.server.store.InMemoryMessageStore;
import com.sanya.server.store.MessageStore;

/**
 * Центральный сервер чата Sanya.
 * Поддерживает обмен сообщениями, файлами и голосовыми событиями.
 */
public class ChatServer {

    private static final Logger log = Logger.getLogger(ChatServer.class.getName());
    private static final int DEFAULT_PORT = 12345;
    private static final Map<ObjectOutputStream, String> clients = new ConcurrentHashMap<>();

    private ServerSocket serverSocket;
    private volatile boolean running;
    private static final Map<String, SignedPreKeyBundle> signedBundles = new ConcurrentHashMap<>();
    private static final Map<String, ObjectOutputStream> userOut = new ConcurrentHashMap<>();
    private static final Map<String, String> userPubB64 = new ConcurrentHashMap<>();
    private final MessageStore messageStore = new InMemoryMessageStore();
    // === Точка входа ===
    public static void main(String[] args) {
        try {
            Files.createDirectories(java.nio.file.Path.of("logs"));
            try (InputStream input = ChatServer.class.getResourceAsStream("/logging.properties")) {
                if (input != null) {
                    LogManager.getLogManager().readConfiguration(input);
                    log.info("Server logging configuration loaded successfully");
                } else {
                    System.err.println("[WARN] logging.properties not found in resources");
                }
            }
        } catch (Exception e) {
            System.err.println("[WARN] Failed to initialize logging: " + e.getMessage());
        }

        try {
            ChatServer server = new ChatServer();
            log.info("Starting ChatServer...");
            server.start();
            log.info("ChatServer started successfully");

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    server.stop();
                    log.info("ChatServer stopped gracefully");
                } catch (Exception ex) {
                    log.log(Level.WARNING, "Error during shutdown", ex);
                }
            }));

        } catch (Exception e) {
            log.severe("Fatal error starting ChatServer: " + e.getMessage());
            log.log(Level.SEVERE, "Stack trace:", e);
            System.exit(1);
        }
    }

    // === Основной запуск ===
    public void start() throws IOException {
        if (running) {
            log.warning("Server already running");
            return;
        }
        serverSocket = new ServerSocket(DEFAULT_PORT);
        running = true;

        log.info("Server listening on port " + DEFAULT_PORT);

        while (running) {
            try {
                Socket socket = serverSocket.accept();
                log.info("Client connected: " + socket.getRemoteSocketAddress());
                new Thread(new ClientHandler(socket), "Client-" + socket.getPort()).start();
            } catch (SocketException e) {
                if (running) log.log(Level.WARNING, "Socket exception during accept", e);
            }
        }
    }

    public void stop() throws IOException {
        if (!running) return;
        running = false;
        log.info("Stopping ChatServer...");

        for (ObjectOutputStream out : clients.keySet()) {
            try {
                out.close();
            } catch (IOException ignored) {
            }
        }

        clients.clear();

        if (serverSocket != null && !serverSocket.isClosed()) {
            serverSocket.close();
        }

        log.info("ChatServer stopped");
    }

    // === Обработка клиента ===
    private  class ClientHandler implements Runnable {
        private final Socket socket;
        private String clientName;
        private ObjectOutputStream out;
        private ObjectInputStream in;

        ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());

                // Первое сообщение от клиента — приветствие (HELLO)
                Message hello = (Message) in.readObject();
                clientName = hello.getFrom();
                clients.put(out, clientName);
                userOut.put(clientName, out);
                log.info("[" + clientName + "] connected");

                for (Object msg : messageStore.retrieve(clientName)) {
                    out.writeObject(msg);
                    out.flush();
                }


                // Оповещаем остальных
                broadcastExcept(out, new Message("SERVER", clientName + " entered the chat", Message.Type.SYSTEM));
                updateUserList();

                // === Основной цикл приёма объектов ===
                while (true) {
                    Object obj = in.readObject();

                    // --- Криптография: публичные ключи клиентов ---
                    if (obj instanceof com.sanya.crypto.SignedPreKeyBundle bundle) {
                        signedBundles.put(bundle.getUsername(), bundle);
                        log.info("Received bundle from " + bundle.getUsername());

                        // Рассылаем остальным
                        broadcastExcept(out, bundle);

                        // Новому клиенту шлём все известные ключи других
                        Map<String, com.sanya.crypto.SignedPreKeyBundle> others = new HashMap<>(signedBundles);
                        others.remove(bundle.getUsername());
                        out.writeObject(others);
                        out.flush();
                        continue;
                    }

                    // --- Основные типы сообщений ---
                    if (obj instanceof Message msg) {
                        if (!"<<<HELLO>>>".equals(msg.getText())) {
                            broadcast(msg);
                        }
                        continue;
                    }

                    if (obj instanceof FileTransferRequest req) {
                        broadcast(req);
                        continue;
                    }

                    if (obj instanceof FileChunk chunk) {
                        broadcast(chunk);
                        continue;
                    }

                    if (obj instanceof com.sanya.events.voice.VoiceRecordingEvent evt) {
                        broadcast(evt);
                        continue;
                    }

                    if (obj instanceof com.sanya.events.voice.VoiceMessageReadyEvent v) {
                        broadcast( v);
                        continue;
                    }

                    if (obj instanceof com.sanya.events.voice.VoicePlayEvent play) {
                        broadcast(play);
                        continue;
                    }
                    else if (obj instanceof com.sanya.crypto.msg.KeyHello hk) {
                        userPubB64.put(hk.username(), hk.x25519PublicKeyB64());
                        var snap = new com.sanya.crypto.msg.KeyDirectoryUpdate(Map.copyOf(userPubB64));
                        broadcast(snap);
                    }
                    else if (obj instanceof com.sanya.crypto.msg.EncryptedDirectMessage dm) {
                        ObjectOutputStream dst = userOut.get(dm.to());
                        ObjectOutputStream self = userOut.get(dm.from());
                        if (dst != null) {
                            try {
                                dst.writeObject(dm);
                                dst.flush();
                            } catch (IOException ignore) {}
                        } else {
                            messageStore.save(dm.to(), dm);
                        }
                        if (self != null && self != dst) {
                            try {
                                self.writeObject(dm);
                                self.flush();
                            } catch (IOException ignore) {}
                        }
                    }

                    // Неизвестный тип
                    log.fine("Unknown object: " + obj.getClass().getName());
                }

            } catch (EOFException | StreamCorruptedException e) {
                log.info("Client disconnected: " + clientName);
            } catch (SocketException e) {
                log.info("Socket reset: " + clientName);
            } catch (Exception e) {
                log.log(Level.WARNING, "Error handling client " + clientName, e);
            } finally {
                handleDisconnect();
                try {
                    if (in != null) in.close();
                } catch (IOException ignored) {
                }
                try {
                    if (out != null) out.close();
                } catch (IOException ignored) {
                }
                try {
                    if (socket != null && !socket.isClosed()) socket.close();
                } catch (IOException ignored) {
                }
            }
        }
        // обёртка
        private void handleDisconnect() {
            if (clientName != null) {
                clients.remove(out);
                signedBundles.remove(clientName);
                broadcast(new Message("SERVER", clientName + " left the chat", Message.Type.SYSTEM));
                updateUserList();
                userOut.remove(clientName);
                userPubB64.remove(clientName);
                log.info("[" + clientName + "] disconnected");
            }
        }


        // === Вспомогательные методы ===
        private static void broadcast(Object obj) {
            clients.keySet().removeIf(out -> {
                try {
                    out.writeObject(obj);
                    out.flush();
                    return false;
                } catch (IOException e) {
                    log.warning("Failed to broadcast to one client: " + e.getMessage());
                    return true;
                }
            });
        }

        private static void broadcastExcept(ObjectOutputStream exclude, Object obj) {
            clients.keySet().removeIf(out -> {
                if (out == exclude) return false;
                try {
                    out.writeObject(obj);
                    out.flush();
                    return false;
                } catch (IOException e) {
                    log.warning("Failed to broadcast (exclude mode): " + e.getMessage());
                    return true;
                }
            });
        }

        private static void updateUserList() {
            broadcast(new UserListUpdatedEvent(List.copyOf(clients.values())));
            log.fine("User list updated, total clients: " + clients.size());
        }
    }
}
