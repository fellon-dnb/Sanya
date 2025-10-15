package com.sanya.events.voice;

import java.io.Serializable;

public record VoiceRecordingEvent(String username, boolean started) implements Serializable {}
