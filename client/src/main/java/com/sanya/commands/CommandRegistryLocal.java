package com.sanya.commands;

import java.util.Set;

public final class CommandRegistryLocal {
    private static final Set<String> COMMANDS = Set.of(
            "/help",
            "/exit",
            "/clear"
    );

    private CommandRegistryLocal() {}

    public static boolean isCommand(String input) {
        if (input == null) return false;
        String firstWord = input.trim().split("\\s+")[0];
        return COMMANDS.contains(firstWord);
    }

    public static Set<String> getCommands() {
        return COMMANDS;
    }
}
