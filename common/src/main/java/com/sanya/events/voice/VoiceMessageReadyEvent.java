package com.sanya.events.voice;

import java.io.Serializable;

public record VoiceMessageReadyEvent(String recipient, byte[] data) implements Serializable {}
