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
    private static final Set<ObjectOutputStream> clients = ConcurrentHashMap.newKeySet();

    public static void main(String[] args) throws IOException {
        Arguments a = Arguments.parse(args);
        port = a.get(int.class, "--port", 12345);

        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8)); // вывод в UTF-8
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("Server started on port " + port);

        while (true) {
            Socket socket = serverSocket.accept();
            new Thread(new ClientHandler(socket)).start();
        }
    }

    static class ClientHandler implements Runnable {
        private final Socket socket;
        private String clientName;

        ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try (
                    ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                    ObjectInputStream in = new ObjectInputStream(socket.getInputStream())
            ) {
                clients.add(out);

                // Первое сообщение от клиента — его имя
                Message joinMsg = (Message) in.readObject();
                clientName = joinMsg.getFrom();

                System.out.println("Client connected: " + clientName + " (" + socket + ")");
                broadcast(new Message("SERVER", clientName + " entered the cаhat", Message.Type.SYSTEM));

                while (true) {
                    Message msg = (Message) in.readObject();
                    System.out.println("Received from " + msg.getFrom() + ": " + msg.getText());
                    broadcast(msg);
                }
            } catch (Exception e) {
                if (clientName != null) {
                    System.out.println("Client disconnected: " + clientName);
                    broadcast(new Message("SERVER", clientName + " left the chat", Message.Type.SYSTEM));
                }
            }
        }
    }

    private static void broadcast(Message msg) {
        clients.removeIf(out -> {
            try {
                out.writeObject(msg);
                out.flush();
                return false;
            } catch (IOException e) {
                return true; // убрать «мертвые» соединения
            }
        });
    }
}
