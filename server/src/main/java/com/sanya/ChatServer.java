package com.sanya;

import com.ancevt.replines.core.argument.Arguments;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ChatServer {
    private static int port;
    private static Set<ObjectOutputStream> clients = ConcurrentHashMap.newKeySet();

    public static void main(String[] args) throws IOException {

        Arguments a = Arguments.parse(args);

        port = a.get(int.class, "--port", 12345);

        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8)); // вывод в UTF-8
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("Сервер запущен на порту " + port);

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
        clients.removeIf(out -> {
            try {
                out.writeObject(msg);
                out.flush();
                return false; // всё ок, оставляем
            } catch (IOException e) {
                return true;  // соединение умерло → убрать из списка
            }
        });
    }
}
