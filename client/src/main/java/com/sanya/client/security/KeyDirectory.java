package com.sanya.client.security;

import com.sanya.crypto.Crypto;

import javax.crypto.SecretKey;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * KeyDirectory — хранилище публичных и сеансовых ключей пользователей.
 * Используется для организации end-to-end шифрования (E2EE) с помощью X25519 и AES-GCM.
 *
 * Назначение:
 *  - Хранить собственную пару ключей клиента (X25519).
 *  - Сохранять публичные ключи других пользователей.
 *  - Автоматически генерировать симметричные AES-ключи для шифрования сообщений.
 *
 * Потокобезопасность:
 *  Все коллекции реализованы через {@link ConcurrentHashMap}.
 */
public final class KeyDirectory implements KeyRepository {

    /** Личная пара ключей X25519 текущего пользователя. */
    private final KeyPair my;

    /** Публичные ключи других пользователей. */
    private final Map<String, PublicKey> pub = new ConcurrentHashMap<>();

    /** Кеш симметричных сеансовых ключей (AES-GCM). */
    private final Map<String, SecretKey> session = new ConcurrentHashMap<>();

    /** Создаёт новую пару ключей X25519. */
    public KeyDirectory() {
        this.my = Crypto.genX25519();
    }

    /** Возвращает собственную пару ключей (X25519). */
    public KeyPair myKeyPair() {
        return my;
    }

    /** Сохраняет публичный ключ пользователя и сбрасывает старый сеансовый ключ. */
    public void putPub(String user, PublicKey k) {
        pub.put(user, k);
        session.remove(user);
    }

    /** Возвращает публичный ключ пользователя. */
    public PublicKey getPub(String user) {
        return pub.get(user);
    }

    /**
     * Возвращает существующий или вычисляет новый сеансовый ключ AES-GCM.
     * Ключ вычисляется через ECDH (X25519) по публичному ключу собеседника.
     */
    public SecretKey getOrDeriveSession(String user) {
        return session.computeIfAbsent(user, u -> {
            PublicKey p = pub.get(u);
            if (p == null) return null;
            return Crypto.deriveX25519(my.getPrivate(), p);
        });
    }

    /** Возвращает копию всех публичных ключей. */
    public Map<String, PublicKey> allPubs() {
        return Map.copyOf(pub);
    }

    /** Реализация интерфейса KeyRepository: сохраняет публичный ключ. */
    @Override
    public void store(String user, PublicKey k) {
        putPub(user, k);
    }

    /** Реализация интерфейса KeyRepository: возвращает публичный ключ. */
    @Override
    public PublicKey get(String user) {
        return getPub(user);
    }
}
