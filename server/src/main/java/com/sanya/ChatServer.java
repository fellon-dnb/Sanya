package com.sanya;

import com.ancevt.replines.core.argument.Arguments;
import com.sanya.events.*;
import com.sanya.files.FileChunk;
import com.sanya.files.FileTransferRequest;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ChatServer — пересылает сообщения, файлы и голосовые пакеты.
 */
public class ChatServer {

    private static int port;
    private static final Map<ObjectOutputStream, String> clients = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {
        Arguments a = Arguments.parse(args);
        port = a.get(int.class, "--port", 12345);

        System.setProperty("file.encoding", "UTF-8");
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        System.out.println("Server started on port " + port);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket socket = serverSocket.accept();
                new Thread(new ClientHandler(socket), "ClientHandler-" + socket.getPort()).start();
            }
        }
    }

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
                in = new ObjectInputStream(socket.getInputStream());

                Message hello = (Message) in.readObject();
                clientName = hello.getFrom();
                clients.put(out, clientName);
                System.out.println("Client connected: " + clientName);

                broadcast(new Message("SERVER", clientName + " entered the chat", Message.Type.SYSTEM));
                updateUserList();

                while (true) {
                    try {
                        Object obj = in.readObject();

                        if (obj instanceof Message msg) {
                            System.out.println("[" + msg.getFrom() + "]: " + msg.getText());
                            broadcast(msg);
                        } else if (obj instanceof FileTransferRequest req) {
                            broadcast(req);
                        } else if (obj instanceof FileChunk chunk) {
                            broadcast(chunk);
                        } else if (obj instanceof VoiceRecordingEvent evt) {
                            broadcast(evt); // уведомление "записывает голосовое..."
                        } else if (obj instanceof VoiceMessageReadyEvent msg) {
                            broadcast(msg); // готовое голосовое сообщение
                        } else if (obj instanceof VoicePlayEvent play) {
                            broadcast(play); // уведомление о прослушивании
                        }
                    } catch (EOFException | StreamCorruptedException e) {
                        break; // клиент закрыл соединение
                    } catch (Exception e) {
                        e.printStackTrace();
                        break;
                    }
                }
                handleDisconnect();
            } catch (Exception e) {
                handleDisconnect();
            }
        }

        private void handleDisconnect() {
            if (clientName != null) {
                System.out.println("Client disconnected: " + clientName);
                clients.remove(out);
                broadcast(new Message("SERVER", clientName + " left the chat", Message.Type.SYSTEM));
                updateUserList();
            }
            try {
                socket.close();
            } catch (IOException ignored) {}
        }
    }

    private static void broadcast(Object obj) {
        clients.keySet().removeIf(out -> {
            try {
                out.writeObject(obj);
                out.flush();
                return false;
            } catch (IOException e) {
                return true;
            }
        });
    }

    private static void updateUserList() {
        broadcast(new UserListUpdatedEvent(List.copyOf(clients.values())));
    }
}
