package com.sanya.client.core;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Простой DI-контейнер для управления зависимостями в клиенте.
 * Позволяет регистрировать и извлекать компоненты (Singleton или Prototype).
 */
public class DependencyContainer {

    private final Map<Class<?>, Supplier<?>> providers = new HashMap<>();
    private final Map<Class<?>, Object> singletons = new HashMap<>();

    /**
     * Регистрирует зависимость как Singleton.
     * При первом запросе создаёт экземпляр и кеширует его.
     */
    public <T> void registerSingleton(Class<T> type, Supplier<T> supplier) {
        providers.put(type, () -> singletons.computeIfAbsent(type, k -> supplier.get()));
    }

    /**
     * Регистрирует зависимость как Prototype (новый экземпляр при каждом вызове get()).
     */
    public <T> void register(Class<T> type, Supplier<T> supplier) {
        providers.put(type, supplier);
    }

    /**
     * Получает экземпляр зарегистрированного типа.
     * Если тип не зарегистрирован — выбрасывает IllegalStateException.
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
     * Проверяет, зарегистрирован ли тип.
     */
    public boolean isRegistered(Class<?> type) {
        return providers.containsKey(type);
    }
}
