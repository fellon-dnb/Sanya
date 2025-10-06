package com.sanya.commands;

import com.ancevt.replines.core.repl.CommandRegistry;
import com.ancevt.replines.core.repl.ReplRunner;
import com.sanya.events.ClearChatEvent;
import com.sanya.events.EventBus;

import javax.swing.*;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class CommandHandler {

    private final ReplRunner replRunner;

    public CommandHandler(JTextArea outputArea, EventBus eventBus) {

        OutputStream outputStream = new OutputStream() {
            @Override
            public void write(int b) {
                SwingUtilities.invokeLater(() -> {
                    // Декодируем символ правильно (в UTF-8)
                    outputArea.append(new String(new byte[]{(byte) b}, StandardCharsets.UTF_8));
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

        registry.command("/clear")
                .action((r, args) -> {
                    r.println("[SYSTEM] Очищаю чат...");
                    eventBus.publish(new ClearChatEvent());
                })
                .build();

        // 💡 Создаём PrintStream с жёстким UTF-8
        PrintStream utf8PrintStream = new PrintStream(outputStream, true, StandardCharsets.UTF_8);

        this.replRunner = ReplRunner.builder()
                .withOutput(utf8PrintStream)
                .withRegistry(registry)
                .build();
    }

    public ReplRunner getReplRunner() {
        return replRunner;
    }
}
