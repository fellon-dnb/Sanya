package com.sanya.commands;

import java.util.Set;

public final class CommandRegistry {
    private static final Set<String> COMMANDS = Set.of(
            "/help",
            "/exit"

    );

    private CommandRegistry() {
        // запретить создание экземпляров
    }

    public static boolean isCommand(String input) {
        if (input == null) return false;
        String firstWord = input.trim().split("\\s+")[0];
        return COMMANDS.contains(firstWord);
    }

    public static Set<String> getCommands() {
        return COMMANDS;
    }
}

