package com.sanya.events;

import java.io.Serializable;
import java.util.List;

public record UserListUpdatedEvent(List<String> usernames) implements Serializable {
}
