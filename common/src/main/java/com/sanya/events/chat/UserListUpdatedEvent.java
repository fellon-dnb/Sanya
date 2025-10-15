package com.sanya.events.chat;

import java.io.Serializable;
import java.util.List;

public record UserListUpdatedEvent(List<String> usernames) implements Serializable {
}
