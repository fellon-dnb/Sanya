package com.sanya.client.security;

import java.security.PublicKey;

/**
 * KeyRepository — интерфейс репозитория публичных ключей пользователей.
 * Определяет операции сохранения и получения ключей для реализации E2EE.
 *
 * Назначение:
 *  - Абстрагировать хранилище ключей (в памяти, базе данных, Redis и т. д.).
 *  - Обеспечить единый контракт для модулей шифрования.
 *
 * Использование:
 *  Реализация — {@link KeyDirectory}, которая хранит ключи в памяти и автоматически
 *  вычисляет сеансовые AES-ключи через X25519.
 */
public interface KeyRepository {

    /**
     * Сохраняет публичный ключ пользователя.
     *
     * @param username имя или идентификатор пользователя
     * @param pubkey   публичный ключ
     */
    void store(String username, PublicKey pubkey);

    /**
     * Возвращает публичный ключ пользователя.
     *
     * @param username имя или идентификатор пользователя
     * @return объект {@link PublicKey}, если ключ найден, иначе null
     */
    PublicKey get(String username);
}
