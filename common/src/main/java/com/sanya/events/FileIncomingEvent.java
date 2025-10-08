package com.sanya.events;

import com.sanya.files.FileTransferRequest;

import java.io.ObjectInputStream;

public record FileIncomingEvent(FileTransferRequest request, ObjectInputStream input) {
}
