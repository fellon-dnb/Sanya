package com.sanya;

import com.ancevt.replines.core.argument.Arguments;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Многопоточный сервер чата.
 * Рассылает системные уведомления о входе/выходе и список активных пользователей.
 */
public class ChatServer {

    private static int port;
    private static final Map<ObjectOutputStream, String> clients = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {
        Arguments a = Arguments.parse(args);
        port = a.get(int.class, "--port", 12345);

        // Глобальная установка UTF-8
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

        ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (
                    ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                    ObjectInputStream in = new ObjectInputStream(socket.getInputStream())
            ) {
                this.out = out;

                // Первое сообщение от клиента — handshake
                Message helloMsg = (Message) in.readObject();
                clientName = helloMsg.getFrom();

                clients.put(out, clientName);
                System.out.println("Client connected: " + clientName);

                // Оповещаем всех о новом участнике
                broadcast(new Message("SERVER", clientName + " entered the chat", Message.Type.SYSTEM));

                // Отправляем обновлённый список активных пользователей
                updateUserList();

                // Основной цикл получения сообщений
                while (true) {
                    Message msg = (Message) in.readObject();
                    System.out.println("[" + msg.getFrom() + "]: " + msg.getText());
                    broadcast(msg);
                }

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
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * Рассылает сообщение всем клиентам.
     */
    private static void broadcast(Message msg) {
        clients.keySet().removeIf(out -> {
            try {
                out.writeObject(msg);
                out.flush();
                return false;
            } catch (IOException e) {
                return true; // клиент больше не активен
            }
        });
    }

    /**
     * Рассылает всем обновлённый список пользователей.
     */
    private static void updateUserList() {
        String userList = String.join(",", clients.values());
        broadcast(new Message("SERVER", "[SERVER] users: " + userList, Message.Type.SYSTEM));
    }
}
