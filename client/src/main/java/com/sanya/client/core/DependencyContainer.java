package com.sanya.client.core;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * DependencyContainer — минималистичный DI-контейнер (Dependency Injection),
 * используемый для управления зависимостями в клиентском приложении.
 *
 * Назначение:
 *  - Позволяет регистрировать и извлекать зависимости по типу.
 *  - Поддерживает два режима: Singleton (один экземпляр) и Prototype (новый экземпляр при каждом запросе).
 *  - Упрощает тестирование и модульность, устраняя жёсткие зависимости между классами.
 *
 * Использование:
 *  ctx.di().registerSingleton(ChatConnector.class, () -> new ChatConnector(...));
 *  ChatConnector connector = ctx.di().get(ChatConnector.class);
 */
public class DependencyContainer {

    /** Провайдеры зависимостей (создают экземпляры по запросу) */
    private final Map<Class<?>, Supplier<?>> providers = new HashMap<>();

    /** Кеш экземпляров для Singleton-зависимостей */
    private final Map<Class<?>, Object> singletons = new HashMap<>();

    /**
     * Регистрирует зависимость как Singleton.
     * При первом запросе создаёт экземпляр и сохраняет его в кеше.
     *
     * @param type     тип зависимости
     * @param supplier фабрика, создающая экземпляр при первом обращении
     * @param <T>      тип возвращаемого объекта
     */
    public <T> void registerSingleton(Class<T> type, Supplier<T> supplier) {
        providers.put(type, () -> singletons.computeIfAbsent(type, k -> supplier.get()));
    }

    /**
     * Регистрирует зависимость как Prototype.
     * Каждый вызов {@link #get(Class)} создаёт новый экземпляр.
     *
     * @param type     тип зависимости
     * @param supplier фабрика, создающая экземпляры
     * @param <T>      тип возвращаемого объекта
     */
    public <T> void register(Class<T> type, Supplier<T> supplier) {
        providers.put(type, supplier);
    }

    /**
     * Возвращает экземпляр зарегистрированного типа.
     * Если тип не зарегистрирован — выбрасывает исключение.
     *
     * @param type тип зависимости
     * @param <T>  ожидаемый тип возвращаемого объекта
     * @return экземпляр зарегистрированной зависимости
     * @throws IllegalStateException если тип не зарегистрирован
     */
    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> type) {
        Supplier<?> supplier = providers.get(type);
        if (supplier == null) {
            throw new IllegalStateException("No provider registered for type: " + type.getName());
        }
        return (T) supplier.get();
    }

    /**
     * Проверяет, зарегистрирован ли указанный тип в контейнере.
     *
     * @param type класс зависимости
     * @return true, если зарегистрирован; иначе false
     */
    public boolean isRegistered(Class<?> type) {
        return providers.containsKey(type);
    }
}
