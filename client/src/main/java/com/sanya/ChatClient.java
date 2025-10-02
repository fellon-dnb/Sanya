package com.sanya;

import com.ancevt.replines.core.argument.Arguments;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class ChatClient {
    public static void main(String[] args) throws Exception {
        Arguments a = Arguments.parse(args);

        String host = a.get(String.class, "--host", "localhost");
        int port = a.get(int.class, "--port", 12345);

        System.out.printf("Подключаюсь к %s:%d...%n", host, port);

        Socket socket = new Socket(host, port);

        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

        Scanner scanner = new Scanner(System.in);
        System.out.print("Введите ваше имя: ");
        String name = scanner.nextLine();

        // ⚡ Сразу отправляем имя на сервер
        out.writeObject(new Message(name, "<<<HELLO>>>"));
        out.flush();

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

        // Ввод и отправка
        while (true) {
            String text = scanner.nextLine();
            out.writeObject(new Message(name, text));
            out.flush();
        }
    }
}
