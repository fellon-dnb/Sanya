package com.sanya.events.voice;

public record VoiceRecordingStartedEvent(String username) implements VoiceEvent {
}
