package com.sanya;

import java.io.Serializable;

    public class Message implements Serializable {
        private String from;
        private String text;

        public Message(String from, String text) {
            this.from = from;
            this.text = text;
        }

        public String getFrom() {
            return from;
        }

        public String getText() {
            return text;
        }

        @Override
        public String toString() {
            return from + ": " + text;
        }
    }


