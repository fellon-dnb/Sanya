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

        eventBus.subscribe(MessageSendEvent.class, e -> safeSendMessage(e.text()));
    }

    public void connect() {
        try {
            socket = new Socket(host, port);
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in  = new ObjectInputStream(socket.getInputStream());
            connected = true;

            out.writeObject(new Message(username, "<<<HELLO>>>"));
            out.flush();

            readerThread = new Thread(this::listenLoop, "ChatClientReader");
            readerThread.start();
        } catch (IOException e) {
            eventBus.publish(new MessageReceivedEvent(
                    new Message("SYSTEM", "Connection failed: " + e.getMessage(), Message.Type.SYSTEM)
            ));
            close();
        }
    }

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
                    if (!"voice".equals(chunk.getFilename())) {
                        eventBus.publish(new FileChunkEvent(chunk));
                    }
                } else if (obj instanceof VoiceMessageReadyEvent evt) {
                    // Важно: НЕ публикуем свои сообщения, они уже обработаны через диалог
                    if (!evt.username().equals(username)) {
                        eventBus.publish(evt);
                    }
                    // Свои сообщения игнорируем
                }
            }
        } catch (EOFException | StreamCorruptedException ignored) {
        } catch (Exception e) {
            eventBus.publish(new MessageReceivedEvent(
                    new Message("SYSTEM", "Read error: " + e.getMessage(), Message.Type.SYSTEM)
            ));
        } finally {
            close();
            eventBus.publish(new UserDisconnectedEvent(username));
        }
    }

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

    public ObjectOutputStream getOutputStream() { return out; }

    public void close() {
        connected = false;
        try { if (out != null) out.close(); } catch (IOException ignored) {}
        try { if (in  != null) in.close();  } catch (IOException ignored) {}
        try { if (socket != null && !socket.isClosed()) socket.close(); } catch (IOException ignored) {}
    }
}
