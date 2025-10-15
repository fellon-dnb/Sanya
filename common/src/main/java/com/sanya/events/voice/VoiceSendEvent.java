package com.sanya.events.voice;

public record VoiceSendEvent(byte[] data, boolean last) {
}
