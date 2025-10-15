package com.sanya.events.voice;

public record VoiceRecordingStatusEvent(String username, boolean recording) implements VoiceEvent {
}
