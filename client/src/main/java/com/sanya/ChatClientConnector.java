package com.sanya;

import com.sanya.events.*;

import javax.swing.*;
import java.io.*;
import java.net.Socket;

public class ChatClientConnector {

    private final String host;
    private final int port;
    private final String name;
    private final EventBus eventBus;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Socket socket;


    public ChatClientConnector(String host, int port, String name, EventBus eventBus) {
        this.host = host;
        this.port = port;
        this.name = name;
        this.eventBus = eventBus;
        //подписка
        eventBus.subscribe(MessageSendEvent.class, e -> sendMessage(e.text()));
    }

    public void connect() {
        new Thread(() -> {
            try {
                socket = new Socket(host, port);
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());
                //сигнал о подключении пользователя
                eventBus.publish(new UseConnectedEvent(name));
                // Сразу после подключения отправляем имя
                out.writeObject(new Message(name, name + " enter the chat", Message.Type.SYSTEM));
                out.flush();


                // Слушаем входящие
                while (true) {
                    Message msg = (Message) in.readObject();
                    eventBus.publish(new MessageReceivedEvent(msg));
                }
            } catch (Exception e) {
               eventBus.publish(new UserDisconnectedEvent(name));
            }
        }, "ChatClient-Listener").start();
    }

    public void sendMessage(String text) {
        try {
            if (out != null) {
                out.writeObject(new Message(name, text));
                out.flush();
            }
        } catch (IOException e) {
            SwingUtilities.invokeLater(() -> {
                eventBus.publish(new MessageReceivedEvent(new Message("SYSTEM", "Ошибка отправки: " + e.getMessage(),Message.Type.SYSTEM)));
            });
        }
    }

    public void close() {
        try {
            socket.close();
        } catch (IOException ignored) {}
    }
}
