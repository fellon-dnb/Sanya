package com.sanya.events.voice;

import java.io.Serializable;

public record VoiceReceivedEvent(byte[] data, boolean last) implements VoiceEvent, Serializable {}
