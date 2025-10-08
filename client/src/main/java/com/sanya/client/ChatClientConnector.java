package com.sanya.client;

import com.sanya.Message;
import com.sanya.events.*;
import com.sanya.files.FileTransferRequest;

import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;

/**
 * ChatClientConnector — отвечает за сетевое взаимодействие клиента:
 * подключение, приём и отправка сообщений, уведомления через EventBus.
 *
 * Полностью независим от UI — работает одинаково
 * для Swing, консоли или будущих клиентов (Qt, Electron и т.д.).
 */
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

        // Подписываемся на событие отправки сообщения
        eventBus.subscribe(MessageSendEvent.class, e -> sendMessage(e.text()));
    }

    /** Подключение к серверу */
    public void connect() {
        new Thread(() -> {
            try {
                socket = new Socket(host, port);
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());

                // уведомляем о подключении
                eventBus.publish(new UserConnectedEvent(username));

                // отправляем приветственное сообщение
                out.writeObject(new Message(username, "<<<HELLO>>>"));
                out.flush();

                // основной цикл приёма данных
                while (true) {
                    Object obj = in.readObject();

                    if (obj instanceof Message msg) {
                        handleSystemOrChatMessage(msg);
                    }
                    else if (obj instanceof FileTransferRequest req) {
                        // ⚡ публикуем событие о входящем файле
                        eventBus.publish(new FileIncomingEvent(req, in));
                    }
                }

            } catch (Exception e) {
                eventBus.publish(new UserDisconnectedEvent(username));
            }
        }, "ChatClient-Listener").start();
    }

    /** Разбор текстовых сообщений (системные, пользовательские, список пользователей) */
    private void handleSystemOrChatMessage(Message msg) {
        String text = msg.getText();

        if (text.contains("entered the chat")) {
            String name = text.replace("[SERVER]", "")
                    .replace("entered the chat", "").trim();
            eventBus.publish(new UserConnectedEvent(name));

        } else if (text.contains("left the chat")) {
            String name = text.replace("[SERVER]", "")
                    .replace("left the chat", "").trim();
            eventBus.publish(new UserDisconnectedEvent(name));

        } else if (text.startsWith("[SERVER] users:")) {
            String list = text.replace("[SERVER] users:", "").trim();
            List<String> users = Arrays.asList(list.split(","));
            eventBus.publish(new UserListUpdatedEvent(users));

        } else {
            eventBus.publish(new MessageReceivedEvent(msg));
        }
    }

    /** Отправка текстового сообщения */
    public void sendMessage(String text) {
        try {
            if (out != null) {
                out.writeObject(new Message(username, text));
                out.flush();
            }
        } catch (IOException e) {
            eventBus.publish(new MessageReceivedEvent(
                    new Message("SYSTEM",
                            "Ошибка отправки: " + e.getMessage(),
                            Message.Type.SYSTEM)
            ));
        }
    }

    /** Закрытие соединения */
    public void close() {
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
    }

    public ObjectOutputStream getOutputStream() {
        return out;
    }
}
