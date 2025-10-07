package com.sanya.events;

import java.util.List;

public record UserListUpdatedEvent(List<String> usernames) {
}
