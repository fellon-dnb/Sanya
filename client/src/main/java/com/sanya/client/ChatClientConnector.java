package com.sanya.client;

import com.sanya.Message;
import com.sanya.events.*;
import com.sanya.files.FileChunk;
import com.sanya.files.FileTransferRequest;

import java.io.*;
import java.net.Socket;

public class ChatClientConnector {

    private final String host;
    private final int port;
    private final String username;
    private final EventBus eventBus;

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Thread readerThread;
    private volatile boolean connected = false;

    public ChatClientConnector(String host, int port, String username, EventBus eventBus) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.eventBus = eventBus;

        // подписка на исходящие текстовые сообщения
        eventBus.subscribe(MessageSendEvent.class, e -> safeSendMessage(e.text()));
    }

    /** Подключение к серверу */
    public void connect() {
        try {
            socket = new Socket(host, port);

            // важно: сначала out, потом flush, потом in
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            connected = true;

            // handshake
            Message hello = new Message(username, "<<<HELLO>>>");
            out.writeObject(hello);
            out.flush();

            // запускаем поток чтения
            readerThread = new Thread(this::listenLoop, "ChatClientReader");
            readerThread.start();

            eventBus.publish(new UserConnectedEvent(username));

        } catch (IOException e) {
            eventBus.publish(new MessageReceivedEvent(
                    new Message("SYSTEM", "Connection failed: " + e.getMessage(), Message.Type.SYSTEM)
            ));
            close();
        } catch (Exception e) {
            eventBus.publish(new MessageReceivedEvent(
                    new Message("SYSTEM", "Unexpected error: " + e.getMessage(), Message.Type.SYSTEM)
            ));
            close();
        }
    }

    /** Основной цикл чтения данных с сервера */
    private void listenLoop() {
        try {
            while (connected && !socket.isClosed()) {
                Object obj = in.readObject();

                if (obj instanceof Message msg) {
                    eventBus.publish(new MessageReceivedEvent(msg));
                } else if (obj instanceof UserListUpdatedEvent list) {
                    eventBus.publish(list);
                } else if (obj instanceof FileTransferRequest req) {
                    eventBus.publish(new FileIncomingEvent(req, in));
                } else if (obj instanceof FileChunk chunk) {
                    if ("voice".equals(chunk.getFilename())) {
                        eventBus.publish(new VoiceReceivedEvent(chunk.getData(), chunk.isLast()));
                    }

                }
                else if (obj instanceof VoiceMessageReadyEvent evt) {
                    eventBus.publish(evt);
                }

            }
        } catch (EOFException | StreamCorruptedException e) {
            // клиент или сервер закрыл соединение
        } catch (Exception e) {
            eventBus.publish(new MessageReceivedEvent(
                    new Message("SYSTEM", "Read error: " + e.getMessage(), Message.Type.SYSTEM)
            ));
        } finally {
            close();
            eventBus.publish(new UserDisconnectedEvent(username));
        }
    }

    /** Отправка текстового сообщения */
    private void safeSendMessage(String text) {
        if (out == null) return;
        try {
            out.writeObject(new Message(username, text));
            out.flush();
        } catch (IOException e) {
            eventBus.publish(new MessageReceivedEvent(
                    new Message("SYSTEM", "Send failed: " + e.getMessage(), Message.Type.SYSTEM)
            ));
        }
    }

    /** Получить поток для отправки файлов/голоса */
    public ObjectOutputStream getOutputStream() {
        return out;
    }

    /** Закрытие соединения */
    public void close() {
        connected = false;
        try { if (out != null) out.close(); } catch (IOException ignored) {}
        try { if (in != null) in.close(); } catch (IOException ignored) {}
        try { if (socket != null && !socket.isClosed()) socket.close(); } catch (IOException ignored) {}
    }
}
