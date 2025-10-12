package com.sanya.events;

public record VoiceRecordingStoppedEvent(String username, byte[] data) implements VoiceEvent {
}
