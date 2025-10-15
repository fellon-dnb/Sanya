package com.sanya.events.voice;

import java.io.Serializable;

public record VoicePlayEvent(String username, String messageId) implements Serializable{
}
