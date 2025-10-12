package com.sanya.events;

public record VoiceRecordingStatusEvent(String username, boolean recording) implements VoiceEvent {
}
