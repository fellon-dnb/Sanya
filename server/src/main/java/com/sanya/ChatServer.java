package com.sanya;

import com.sanya.events.chat.UserListUpdatedEvent;
import com.sanya.events.voice.VoiceMessageReadyEvent;
import com.sanya.events.voice.VoicePlayEvent;
import com.sanya.events.voice.VoiceRecordingEvent;
import com.sanya.files.FileChunk;
import com.sanya.files.FileTransferRequest;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

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
            try { out.close(); } catch (IOException ignored) {}
        }

        clients.clear();

        if (serverSocket != null && !serverSocket.isClosed()) {
            serverSocket.close();
        }

        log.info("ChatServer stopped");
    }

    // === Обработка клиента ===
    private static class ClientHandler implements Runnable {
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
                in  = new ObjectInputStream(socket.getInputStream());

                Message hello = (Message) in.readObject();
                clientName = hello.getFrom();
                clients.put(out, clientName);
                log.info("[" + clientName + "] connected");

                broadcastExcept(out, new Message("SERVER", clientName + " entered the chat", Message.Type.SYSTEM));
                updateUserList();

                while (true) {
                    try {
                        Object obj = in.readObject();

                        if (obj instanceof Message msg) {
                            if (!"<<<HELLO>>>".equals(msg.getText())) broadcast(msg);
                        } else if (obj instanceof FileTransferRequest req) {
                            broadcast(req);
                        } else if (obj instanceof FileChunk chunk) {
                            broadcast(chunk);
                        } else if (obj instanceof VoiceRecordingEvent evt) {
                            broadcast(evt);
                        } else if (obj instanceof VoiceMessageReadyEvent v) {
                            broadcastExcept(out, v);
                        } else if (obj instanceof VoicePlayEvent play) {
                            broadcast(play);
                        }

                    } catch (EOFException | StreamCorruptedException e) {
                        break;
                    } catch (SocketException e) {
                        log.info("Client disconnected (socket reset): " + clientName);
                        break;
                    } catch (Exception e) {
                        log.log(Level.WARNING, "Error handling client " + clientName, e);
                        break;
                    }
                }

            } catch (Exception e) {
                log.log(Level.SEVERE, "Client connection error", e);
            } finally {
                handleDisconnect();
                try { if (in != null) in.close(); } catch (IOException ignored) {}
                try { if (out != null) out.close(); } catch (IOException ignored) {}
                try { if (socket != null && !socket.isClosed()) socket.close(); } catch (IOException ignored) {}
            }
        }

        private void handleDisconnect() {
            if (clientName != null) {
                clients.remove(out);
                broadcast(new Message("SERVER", clientName + " left the chat", Message.Type.SYSTEM));
                updateUserList();
                log.info("[" + clientName + "] disconnected");
            }
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
