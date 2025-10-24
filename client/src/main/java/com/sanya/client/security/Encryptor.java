package com.sanya.client.security;

import com.sanya.client.core.api.CryptoEngine;
import com.sanya.crypto.Crypto;

import javax.crypto.SecretKey;

/**
 * Encryptor — реализация {@link CryptoEngine}, обеспечивающая шифрование и расшифровку данных
 * с использованием симметричных сеансовых ключей (AES-GCM), управляемых {@link KeyDirectory}.
 *
 * Назначение:
 *  - Обеспечить E2EE (end-to-end encryption) между пользователями.
 *  - Автоматически получать или вычислять сеансовые ключи по публичным ключам (через X25519).
 *  - Предоставлять простой интерфейс для шифрования/дешифрования произвольных байтов.
 *
 * Использование:
 *  Вызывается из {@link com.sanya.client.net.ChatConnector} при отправке и получении сообщений.
 */
public final class Encryptor implements CryptoEngine {

    /** Локальный каталог ключей (публичные и сеансовые ключи пользователей). */
    private final KeyDirectory dir;

    /**
     * Создаёт новый экземпляр Encryptor.
     *
     * @param dir каталог ключей для обмена и хранения сеансовых ключей
     */
    public Encryptor(KeyDirectory dir) {
        this.dir = dir;
    }

    /**
     * Вспомогательная запись, содержащая nonce и ciphertext.
     */
    public record Enc(byte[] nonce, byte[] ct) {}

    /**
     * Шифрует данные для указанного пользователя.
     * Использует или создаёт сеансовый ключ.
     *
     * @param toUser идентификатор получателя
     * @param plain  исходные данные
     * @return объект {@link Enc}, содержащий nonce и зашифрованные данные
     */
    public Enc encryptFor(String toUser, byte[] plain) {
        SecretKey k = dir.getOrDeriveSession(toUser);
        if (k == null) throw new IllegalStateException("No pubkey for " + toUser);
        byte[] n = Crypto.randomNonce12();
        return new Enc(n, Crypto.encryptGCM(k, n, plain));
    }

    /**
     * Расшифровывает данные, полученные от указанного пользователя.
     *
     * @param fromUser идентификатор отправителя
     * @param nonce    одноразовый вектор инициализации (12 байт)
     * @param ct       зашифрованный текст
     * @return расшифрованные байты
     */
    public byte[] decryptFrom(String fromUser, byte[] nonce, byte[] ct) {
        SecretKey k = dir.getOrDeriveSession(fromUser);
        if (k == null) throw new IllegalStateException("No pubkey for " + fromUser);
        return Crypto.decryptGCM(k, nonce, ct);
    }

    /**
     * Унифицированный метод шифрования для интерфейса {@link CryptoEngine}.
     * Возвращает массив, где первые 12 байт — nonce, остальные — ciphertext.
     */
    @Override
    public byte[] encrypt(byte[] data, String recipient) {
        Enc e = encryptFor(recipient, data);
        byte[] out = new byte[e.nonce.length + e.ct.length];
        System.arraycopy(e.nonce, 0, out, 0, e.nonce.length);
        System.arraycopy(e.ct, 0, out, e.nonce.length, e.ct.length);
        return out;
    }

    /**
     * Унифицированный метод расшифровки для интерфейса {@link CryptoEngine}.
     * Ожидает формат: nonce (12 байт) + ciphertext.
     */
    @Override
    public byte[] decrypt(byte[] data, String sender) {
        if (data.length < 12) throw new IllegalArgumentException("Invalid encrypted payload");
        byte[] nonce = new byte[12];
        byte[] ct = new byte[data.length - 12];
        System.arraycopy(data, 0, nonce, 0, 12);
        System.arraycopy(data, 12, ct, 0, ct.length);
        return decryptFrom(sender, nonce, ct);
    }
}
