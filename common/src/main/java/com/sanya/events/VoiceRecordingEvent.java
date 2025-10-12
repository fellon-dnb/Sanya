package com.sanya.events;

import java.io.Serializable;

public record VoiceRecordingEvent(String username, boolean started) implements Serializable {}
