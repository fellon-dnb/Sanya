package com.sanya.events;

public record VoiceRecordingStartedEvent(String username) implements VoiceEvent {
}
