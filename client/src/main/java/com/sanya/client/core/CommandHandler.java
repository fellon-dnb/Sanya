package com.sanya.client.core;

import com.ancevt.replines.core.argument.Arguments;
import com.ancevt.replines.core.repl.ReplRunner;
import com.ancevt.replines.core.repl.annotation.ReplCommand;
import com.sanya.client.ApplicationContext;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

public class CommandHandler {

    private final ReplRunner replRunner;

    public CommandHandler(ApplicationContext ctx) {
        replRunner = ReplRunner.builder()
                .withOutput(new PrintStream(System.out, true, StandardCharsets.UTF_8))
                .withCommandFilterPrefix("/")
                .configure(reg -> reg.register(new CommandDefinitions(ctx)))
                .build();
    }

    public ReplRunner getReplRunner() {
        return replRunner;
    }

    public static class CommandDefinitions {
        private final ApplicationContext ctx;

        public CommandDefinitions(ApplicationContext ctx) {
            this.ctx = ctx;
        }

        @ReplCommand(name = "/exit", description = "Exit from client")
        public void exit(ReplRunner repl, Arguments args) {
            repl.println("[SYSTEM] Завершение работы клиента...");
            System.exit(0);
        }

        @ReplCommand(name = "/help", description = "Show help")
        public void help(ReplRunner repl, Arguments args) {
            repl.println(repl.getRegistry().formattedCommandList());
        }

        @ReplCommand(name = "/clear", description = "Clear chat area")
        public void clear(ReplRunner repl, Arguments args) {
            repl.println("[SYSTEM] Очищаю чат...");
            ctx.services().chat().clearChat();
        }
    }
}
