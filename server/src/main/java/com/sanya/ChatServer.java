package com.sanya;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ChatServer {
    private static final int PORT = 12345;
    private static Set<ObjectOutputStream> clients = ConcurrentHashMap.newKeySet();

    public static void main(String[] args) throws IOException {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8)); // вывод в UTF-8
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Сервер запущен на порту " + PORT);

        while (true) {
            Socket socket = serverSocket.accept();
            System.out.println("Клиент подключился: " + socket);
            new Thread(new ClientHandler(socket)).start();
        }
    }

    static class ClientHandler implements Runnable {
        private Socket socket;

        ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try (
                    ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                    ObjectInputStream in = new ObjectInputStream(socket.getInputStream())
            ) {
                clients.add(out);

                while (true) {
                    Message msg = (Message) in.readObject();
                    System.out.println("Получено: " + msg);
                    broadcast(msg);
                }
            } catch (Exception e) {
                System.out.println("Клиент отключился");
            }
        }
    }

    private static void broadcast(Message msg) {
        for (ObjectOutputStream client : clients) {
            try {
                client.writeObject(msg);
                client.flush();
            } catch (IOException ignored) {}
        }
    }
}
