package com.sanya.client.commands;

import com.ancevt.replines.core.repl.CommandRegistry;
import com.ancevt.replines.core.repl.ReplRunner;
import com.sanya.events.ClearChatEvent;
import com.sanya.events.EventBus;

public class CommandHandler {

    private final ReplRunner replRunner;

    public CommandHandler(EventBus eventBus) {

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
                .build();
    }

    public ReplRunner getReplRunner() {
        return replRunner;
    }
}
