package com.sanya.events;

public record VoiceSendEvent(byte[] data, boolean last) {
}
