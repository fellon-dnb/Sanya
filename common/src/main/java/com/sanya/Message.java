package com.sanya;

import java.io.Serializable;

public class Message implements Serializable {

    public enum Type {
        USER,   // обычное сообщение от пользователя
        SYSTEM  // системное (например, вошёл/вышел из чата)
    }

    private final String from;
    private final String text;
    private final Type type;
     private Object attachment;
    public Message(String from, String text) {
        this(from, text, Type.USER);
    }

    public Message(String from, String text, Type type) {
        this.from = from;
        this.text = text;
        this.type = type;
    }

    public String getFrom() {
        return from;
    }

    public String getText() {
        return text;
    }

    public Type getType() {
        return type;
    }
    public Object getAttachment() {
        return attachment;
    }

    public void setAttachment(Object attachment) {
        this.attachment = attachment;
    }
    @Override
    public String toString() {
        if (type == Type.SYSTEM) {
            return "[SYSTEM] " + text;
        }
        return from + ": " + text;
    }
}
