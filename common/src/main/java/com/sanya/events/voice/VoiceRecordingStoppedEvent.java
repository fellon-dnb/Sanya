package com.sanya.events.voice;

public record VoiceRecordingStoppedEvent(String username, byte[] data) implements VoiceEvent {
}
