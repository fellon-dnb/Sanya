package com.sanya.commands;

import com.ancevt.replines.core.repl.ReplRunner;
import com.ancevt.replines.core.repl.CommandRegistry;
import com.sanya.events.ClearChatEvent;
import com.sanya.events.EventBus;

import javax.swing.*;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

public class CommandHandler {

    private final ReplRunner replRunner;

    public CommandHandler(JTextArea outputArea, EventBus eventBus) {

        OutputStream outputStream = new OutputStream() {
            @Override
            public void write(int b) {
                SwingUtilities.invokeLater(() -> {
                    outputArea.append(String.valueOf((char) b));
                    outputArea.setCaretPosition(outputArea.getDocument().getLength());
                });
            }
        };

        CommandRegistry registry = new CommandRegistry();

        registry.command("/exit")
                .action((r, args) -> {
                    r.println("[SYSTEM] Завершение работы клиента...");
                    System.exit(0);
                })
                .build();

        registry.command("/help")
                .action((r, args) -> {
                    r.println("[SYSTEM] Доступные команды:");
                    CommandRegistryLocal.getCommands().forEach(cmd -> r.println("  " + cmd));
                })
                .build();

        // ✅ Новая команда /clear
        registry.command("/clear")
                .action((r, args) -> {
                    r.println("[SYSTEM] Очищаю чат...");
                    eventBus.publish(new ClearChatEvent());
                })
                .build();

        this.replRunner = ReplRunner.builder()
                .withOutput(new PrintStream(outputStream, true, StandardCharsets.UTF_8))
                .withRegistry(registry)
                .build();
    }

    public ReplRunner getReplRunner() {
        return replRunner;
    }
}
