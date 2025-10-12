package com.sanya.events;

import java.io.Serializable;

public record VoiceReceivedEvent(byte[] data, boolean last) implements VoiceEvent, Serializable {}
