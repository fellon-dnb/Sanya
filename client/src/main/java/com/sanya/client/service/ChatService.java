package com.sanya.client.service;

import com.sanya.events.*;

public class ChatService {
    private final EventBus bus;

    public ChatService(EventBus bus) {
        this.bus = bus;
    }

    public void sendMessage(String text) {
        bus.publish(new MessageSendEvent(text));
    }

    public void clearChat() {
        bus.publish(new ClearChatEvent());
    }

    public void onMessageReceived(EventHandler<MessageReceivedEvent> handler) {
        bus.subscribe(MessageReceivedEvent.class, handler);
    }

    public void onUserListUpdated(EventHandler<UserListUpdatedEvent> handler) {
        bus.subscribe(UserListUpdatedEvent.class, handler);
    }
}
