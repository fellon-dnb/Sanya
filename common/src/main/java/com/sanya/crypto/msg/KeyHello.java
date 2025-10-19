package com.sanya.crypto.msg;

import java.io.Serializable;

// От клиента серверу сразу после HELLO: переносит pubkey отправителя
public record KeyHello(String username, String x25519PublicKeyB64) implements Serializable {

}
