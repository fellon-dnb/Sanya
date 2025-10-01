package com.sanya;

import javax.swing.*;
import java.io.*;
import java.net.Socket;

public class ChatClientConnector {

    private final String host;
    private final int port;
    private final String name;

    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Socket socket;

    private final ChatUiCallback callback;

    public ChatClientConnector(String host, int port, String name, ChatUiCallback callback) {
        this.host = host;
        this.port = port;
        this.name = name;
        this.callback = callback;
    }

    public void connect() {
        new Thread(() -> {
            try {
                socket = new Socket(host, port);
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());

                // Слушаем входящие
                while (true) {
                    Message msg = (Message) in.readObject();
                    SwingUtilities.invokeLater(() -> callback.onMessage(msg));
                }
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> callback.onError(e));
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
            SwingUtilities.invokeLater(() -> callback.onError(e));
        }
    }

    public void close() {
        try {
            socket.close();
        } catch (IOException ignored) {}
    }
}
