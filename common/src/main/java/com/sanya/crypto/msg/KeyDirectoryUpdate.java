package com.sanya.crypto.msg;

import java.io.Serializable;
import java.util.Map;

// От сервера клиенту: snapshot публичных ключей всех пользователей
public record KeyDirectoryUpdate(Map<String, String> userToX25519PubB64) implements Serializable {}
