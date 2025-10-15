package com.sanya.events;

public record ConnectionLostEvent(String reason, boolean willReconnect) {
}
