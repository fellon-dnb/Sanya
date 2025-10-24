package com.sanya.client.settings;

/**
 * NetworkSettings — класс для хранения сетевых параметров подключения клиента.
 * Содержит адрес хоста и порт сервера.
 *
 * Назначение:
 *  - Инкапсулировать сетевые настройки в одном объекте.
 *  - Проверять корректность значений при создании.
 *
 * Использование:
 *  NetworkSettings net = new NetworkSettings("localhost", 12345);
 *  socket.connect(net.getHost(), net.getPort());
 */
public final class NetworkSettings {

    private String host;
    private int port;

    /**
     * Создаёт объект с указанными параметрами подключения.
     *
     * @param host адрес сервера
     * @param port порт сервера (должен быть > 0)
     */
    public NetworkSettings(String host, int port) {
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("Host cannot be null or blank");
        }
        if (port <= 0) {
            throw new IllegalArgumentException("Port must be greater than 0");
        }
        this.host = host;
        this.port = port;
    }

    /** Возвращает адрес сервера. */
    public String getHost() {
        return host;
    }

    /** Устанавливает адрес сервера. */
    public void setHost(String host) {
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("Host cannot be null or blank");
        }
        this.host = host;
    }

    /** Возвращает порт сервера. */
    public int getPort() {
        return port;
    }

    /** Устанавливает порт сервера. */
    public void setPort(int port) {
        if (port <= 0) {
            throw new IllegalArgumentException("Port must be greater than 0");
        }
        this.port = port;
    }

    @Override
    public String toString() {
        return "NetworkSettings{" +
                "host='" + host + '\'' +
                ", port=" + port +
                '}';
    }
}
