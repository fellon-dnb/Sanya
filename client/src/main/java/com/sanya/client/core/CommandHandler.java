package com.sanya.client.core;

import com.ancevt.replines.core.argument.Arguments;
import com.ancevt.replines.core.repl.ReplRunner;
import com.ancevt.replines.core.repl.annotation.ReplCommand;
import com.sanya.client.ApplicationContext;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Обёртка над REPL с логированием команд.
 */
public class CommandHandler {

    private static final Logger log = Logger.getLogger(CommandHandler.class.getName());
    private final ReplRunner replRunner;

    public CommandHandler(ApplicationContext ctx) {
        replRunner = ReplRunner.builder()
                .withOutput(new PrintStream(System.out, true, StandardCharsets.UTF_8))
                .withCommandFilterPrefix("/")
                .configure(reg -> reg.register(new CommandDefinitions(ctx)))
                .build();
        log.info("CommandHandler initialized");
    }

    public ReplRunner getReplRunner() {
        return replRunner;
    }

    public static class CommandDefinitions {
        private static final Logger log = Logger.getLogger(CommandDefinitions.class.getName());
        private final ApplicationContext ctx;

        public CommandDefinitions(ApplicationContext ctx) {
            this.ctx = ctx;
        }

        @ReplCommand(name = "/exit", description = "Exit from client")
        public void exit(ReplRunner repl, Arguments args) {
            log.info("Received command /exit");
            repl.println("[SYSTEM] Завершение работы клиента...");
            System.exit(0);
        }

        @ReplCommand(name = "/help", description = "Show help")
        public void help(ReplRunner repl, Arguments args) {
            log.info("Received command /help");
            repl.println(repl.getRegistry().formattedCommandList());
        }

        @ReplCommand(name = "/clear", description = "Clear chat area")
        public void clear(ReplRunner repl, Arguments args) {
            log.info("Received command /clear");
            repl.println("[SYSTEM] Очищаю чат...");
            try {
                ctx.services().chat().clearChat();
                log.fine("Chat cleared successfully via /clear");
            } catch (Exception e) {
                log.log(Level.WARNING, "Failed to clear chat via /clear", e);
                repl.println("[ERROR] Не удалось очистить чат: " + e.getMessage());
            }
        }
    }
}
