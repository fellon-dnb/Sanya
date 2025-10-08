package com.sanya.client.commands;

import com.ancevt.replines.core.repl.CommandRegistry;
import com.ancevt.replines.core.repl.ReplRunner;
import com.sanya.client.ApplicationContext;
import com.sanya.events.ClearChatEvent;
import com.sanya.events.EventBus;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

public class CommandHandler {

    private final ReplRunner replRunner;
    private final ApplicationContext ctx;

    public CommandHandler(ApplicationContext ctx) {
        this.ctx = ctx;

        EventBus eventBus = ctx.getEventBus();

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
                    r.println(registry.formattedCommandList());
                })
                .build();

        registry.command("/clear")
                .action((r, args) -> {
                    r.println("[SYSTEM] Очищаю чат...");
                    eventBus.publish(new ClearChatEvent());
                })
                .build();



        replRunner = ReplRunner.builder()
                .withRegistry(registry)
                .withOutput(new PrintStream(System.out, true, StandardCharsets.UTF_8))
                .build();
    }

    public ReplRunner getReplRunner() {
        return replRunner;
    }
}
