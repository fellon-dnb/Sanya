package com.sanya;

public interface ChatUiCallback {
    void onMessage(Message message);
    void onError(Throwable t);
}
