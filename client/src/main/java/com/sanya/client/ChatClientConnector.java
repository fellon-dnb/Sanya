package com.sanya.client;

import com.sanya.Message;
import com.sanya.client.ui.NotificationManager;
import com.sanya.events.*;
import com.sanya.files.FileChunk;
import com.sanya.files.FileTransferEvent;
import com.sanya.files.FileTransferRequest;

import javax.swing.*;
import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;

/**
 * Клиентский сетевой слой: подключение, отправка/приём сообщений и файлов.
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
        eventBus.subscribe(MessageSendEvent.class, e -> sendMessage(e.text()));
    }

    public void connect() {
        new Thread(() -> {
            try {
                socket = new Socket(host, port);
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());

                eventBus.publish(new UserConnectedEvent(username));
                out.writeObject(new Message(username, "<<<HELLO>>>"));
                out.flush();

                while (true) {
                    Object obj = in.readObject();

                    if (obj instanceof Message msg) {
                        handleSystemOrChatMessage(msg);
                    } else if (obj instanceof FileTransferRequest req) {
                        // Пришёл запрос на получение файла
                        handleIncomingFile(req, in, eventBus);
                    }
                }

            } catch (Exception e) {
                eventBus.publish(new UserDisconnectedEvent(username));
            }
        }, "ChatClient-Listener").start();
    }

    private void handleSystemOrChatMessage(Message msg) {
        String text = msg.getText();

        if (text.contains("entered the chat")) {
            String name = text.replace("[SERVER]", "").replace("entered the chat", "").trim();
            eventBus.publish(new UserConnectedEvent(name));

        } else if (text.contains("left the chat")) {
            String name = text.replace("[SERVER]", "").replace("left the chat", "").trim();
            eventBus.publish(new UserDisconnectedEvent(name));

        } else if (text.startsWith("[SERVER] users:")) {
            String list = text.replace("[SERVER] users:", "").trim();
            List<String> users = Arrays.asList(list.split(","));
            eventBus.publish(new UserListUpdatedEvent(users));

        } else {
            eventBus.publish(new MessageReceivedEvent(msg));
        }
    }

    /**
     * Приём файла с выбором пути сохранения и публикацией прогресса.
     */
    private void handleIncomingFile(FileTransferRequest req, ObjectInputStream in, EventBus eventBus) {
        SwingUtilities.invokeLater(() -> {
            try {
                // 🗂️ Диалог выбора пути сохранения
                JFileChooser chooser = new JFileChooser();
                chooser.setDialogTitle("Сохранить файл: " + req.getFilename());
                chooser.setSelectedFile(new File(req.getFilename()));

                int result = chooser.showSaveDialog(null);
                if (result != JFileChooser.APPROVE_OPTION) {
                    NotificationManager.showWarning("❌ Передача файла отменена пользователем.");
                    return;
                }

                File saveFile = chooser.getSelectedFile();
                long total = req.getSize();
                long received = 0;

                eventBus.publish(new FileTransferEvent(
                        FileTransferEvent.Type.STARTED,
                        saveFile.getName(),
                        0,
                        total,
                        false,
                        "Receiving from " + req.getSender()
                ));

                try (FileOutputStream fos = new FileOutputStream(saveFile)) {
                    while (true) {
                        Object chunkObj = in.readObject();
                        if (!(chunkObj instanceof FileChunk chunk)) break;

                        fos.write(chunk.getData());
                        received += chunk.getData().length;

                        eventBus.publish(new FileTransferEvent(
                                FileTransferEvent.Type.PROGRESS,
                                saveFile.getName(),
                                received,
                                total,
                                false,
                                null
                        ));

                        if (chunk.isLast()) break;
                    }
                }

                eventBus.publish(new FileTransferEvent(
                        FileTransferEvent.Type.COMPLETED,
                        saveFile.getName(),
                        total,
                        total,
                        false,
                        null
                ));

                NotificationManager.showInfo("📥 Файл получен: " + saveFile.getAbsolutePath());

            } catch (Exception e) {
                eventBus.publish(new FileTransferEvent(
                        FileTransferEvent.Type.FAILED,
                        req.getFilename(),
                        0,
                        req.getSize(),
                        false,
                        e.getMessage()
                ));
                e.printStackTrace();
            }
        });
    }

    public void sendMessage(String text) {
        try {
            if (out != null) {
                out.writeObject(new Message(username, text));
                out.flush();
            }
        } catch (IOException e) {
            SwingUtilities.invokeLater(() -> eventBus.publish(new MessageReceivedEvent(
                    new Message("SYSTEM", "Ошибка при отправке: " + e.getMessage(), Message.Type.SYSTEM)
            )));
        }
    }

    public void close() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ignored) {
        }
    }

    public ObjectOutputStream getOutputStream() {
        return out;
    }
}
