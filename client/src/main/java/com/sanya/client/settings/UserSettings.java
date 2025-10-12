package com.sanya.client.settings;

public class UserSettings {

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "UserSettings{" +
                "name='" + name + '\'' +
                '}';
    }
}
