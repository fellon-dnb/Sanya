package com.sanya.messages;

import java.io.Serializable;

public record VoiceMessage(String recipient, byte[] data) implements Serializable {}
