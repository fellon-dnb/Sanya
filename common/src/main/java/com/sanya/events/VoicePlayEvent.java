package com.sanya.events;

import java.io.Serializable;

public record VoicePlayEvent(String username, String messageId) implements Serializable{
}
