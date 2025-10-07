package com.sanya.client;

import com.sanya.Message;
import com.sanya.events.*;

import javax.swing.*;
import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;

public class ChatClientConnector {

    private final String host;
    private final int port;
    private final String username;
    private final EventBus eventBus;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    public ChatClientConnector(String host, int port, String username, EventBus eventBus) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.eventBus = eventBus;

        // Подписываемся на событие "отправить сообщение"
        eventBus.subscribe(MessageSendEvent.class, e -> sendMessage(e.text()));
    }

    public void connect() {
        new Thread(() -> {
            try {
                socket = new Socket(host, port);
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());

                // Сообщаем о подключении
                eventBus.publish(new UserConnectedEvent(username));

                // Уведомляем сервер
                out.writeObject(new Message(username, username + " entered the chat", Message.Type.SYSTEM));
                out.flush();

                // Основной цикл чтения сообщений
                while (true) {
                    Message msg = (Message) in.readObject();

                    if (msg.getType() == Message.Type.SYSTEM) {
                        handleSystemMessage(msg);
                    } else {
                        eventBus.publish(new MessageReceivedEvent(msg));
                    }
                }

            } catch (Exception e) {
                eventBus.publish(new UserDisconnectedEvent(username));
            }
        }, "ChatClient-Listener").start();
    }

    private void handleSystemMessage(Message msg) {
        String text = msg.getText();

        if (text.contains("entered the chat")) {
            // Пример: "[SERVER] Alice entered the chat"
            String name = text.replace("[SERVER]", "").replace("entered the chat", "").trim();
            eventBus.publish(new UserConnectedEvent(name));

        } else if (text.contains("left the chat")) {
            String name = text.replace("[SERVER]", "").replace("left the chat", "").trim();
            eventBus.publish(new UserDisconnectedEvent(name));

        } else if (text.startsWith("[SERVER] users:")) {
            // Пример: "[SERVER] users: Alice,Bob,Charlie"
            String list = text.replace("[SERVER] users:", "").trim();
            List<String> users = Arrays.asList(list.split(","));
            eventBus.publish(new UserListUpdatedEvent(users));

        } else {
            // Обычное системное сообщение
            eventBus.publish(new MessageReceivedEvent(msg));
        }
    }

    public void sendMessage(String text) {
        try {
            if (out != null) {
                out.writeObject(new Message(username, text));
                out.flush();
            }
        } catch (IOException e) {
            SwingUtilities.invokeLater(() -> {
                eventBus.publish(new MessageReceivedEvent(
                        new Message("SYSTEM", "Ошибка отправки сообщения: " + e.getMessage(), Message.Type.SYSTEM)
                ));
            });
        }
    }

    public void close() {
        try {
            socket.close();
        } catch (IOException ignored) {}
    }
}
