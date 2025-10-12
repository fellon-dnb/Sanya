package com.sanya.client;

public class ConnectionInfo {

    private String host;
    private int port;

    public ConnectionInfo(String host, int port) {
        if (port > 0) {
            this.host = host;
            this.port = port;
        } else {
            throw new IllegalArgumentException("Port must be greater than 0");
        }
        if (host == null) {
            throw new IllegalArgumentException("Host cannot be null");
        }
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public String toString() {
        return "ConnectionInfo{" +
                "host='" + host + '\'' +
                ", port=" + port +
                '}';
    }
}
