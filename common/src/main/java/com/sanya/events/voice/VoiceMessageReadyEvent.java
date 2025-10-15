package com.sanya.events.voice;

import java.io.Serializable;

public record VoiceMessageReadyEvent(String username, byte[] data) implements Serializable {}
