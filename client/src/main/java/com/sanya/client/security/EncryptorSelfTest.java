package com.sanya.client.security;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * Простой тест без JUnit:
 * проверяет, что шифрование и расшифровка AES-GCM (через X25519)
 * работают корректно.
 */
public final class EncryptorSelfTest {

    public static void main(String[] args) {
        System.out.println("[EncryptorSelfTest] Starting E2EE self-test...");

        // 1. Создаём два клиента
        KeyDirectory aliceKeys = new KeyDirectory();
        KeyDirectory bobKeys   = new KeyDirectory();

        // 2. Обмениваемся публичными ключами
        bobKeys.putPub("alice", aliceKeys.myKeyPair().getPublic());
        aliceKeys.putPub("bob",   bobKeys.myKeyPair().getPublic());

        // 3. Проверяем совпадение сессионных AES-ключей
        SecretKey aliceSession = aliceKeys.getOrDeriveSession("bob");
        SecretKey bobSession   = bobKeys.getOrDeriveSession("alice");

        if (!java.util.Arrays.equals(aliceSession.getEncoded(), bobSession.getEncoded())) {
            throw new AssertionError("❌ Session keys differ — ECDH failed");
        }

        // 4. Создаём шифраторы
        Encryptor aliceEnc = new Encryptor(aliceKeys);
        Encryptor bobEnc   = new Encryptor(bobKeys);

        // 5. Сообщение
        String msg = "Sanya AES-GCM / X25519 test message ✅";

        // 6. Alice шифрует → Bob расшифровывает
        var enc = aliceEnc.encryptFor("bob", msg.getBytes(StandardCharsets.UTF_8));
        byte[] dec = bobEnc.decryptFrom("alice", enc.nonce(), enc.ct());
        String result = new String(dec, StandardCharsets.UTF_8);

        // 7. Проверяем результат
        if (!msg.equals(result)) {
            throw new AssertionError("❌ Decrypted text mismatch: " + result);
        }

        System.out.println("✅ Encrypt / decrypt test OK");

// Выводим сессионный ключ в hex
        System.out.print("Session key: ");
        for (byte b : aliceSession.getEncoded()) {
            System.out.printf("%02X", b);
        }
        System.out.println();
    }
}
