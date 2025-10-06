package com.sanya.client;

import com.sanya.Message;

public interface ChatUiCallback {
    void onMessage(Message message);
    void onError(Throwable t);
}
