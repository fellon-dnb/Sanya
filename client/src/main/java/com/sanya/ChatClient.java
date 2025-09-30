package com.sanya;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class ChatClient {
    private static final String HOST = "localhost";
    private static final int PORT = 12345;

    public static void main(String[] args) throws Exception {
        Socket socket = new Socket(HOST, PORT);

        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

        // Поток для приёма сообщений
        new Thread(() -> {
            try {
                while (true) {
                    Message msg = (Message) in.readObject();
                    System.out.println(msg);
                }
            } catch (Exception e) {
                System.out.println("Соединение закрыто");
            }
        }).start();

        // Отправка сообщений
        Scanner scanner = new Scanner(System.in);
        System.out.print("Введите ваше имя: ");
        String name = scanner.nextLine();

        while (true) {
            String text = scanner.nextLine();
            out.writeObject(new Message(name, text));
            out.flush();
        }
    }
}
