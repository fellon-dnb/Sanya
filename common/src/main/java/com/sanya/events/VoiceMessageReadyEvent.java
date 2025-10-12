package com.sanya.events;

import java.io.Serializable;

public record VoiceMessageReadyEvent(String username, byte[] data) implements Serializable {}
