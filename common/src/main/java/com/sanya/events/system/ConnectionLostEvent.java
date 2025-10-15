package com.sanya.events.system;

public record ConnectionLostEvent(String reason, boolean willReconnect) {
}
