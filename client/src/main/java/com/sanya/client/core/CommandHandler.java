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
 * CommandHandler — обёртка над REPL-интерфейсом (Read-Eval-Print Loop),
 * обеспечивающая поддержку интерактивных команд клиента.
 *
 * Назначение:
 *  - Предоставить пользователю возможность управлять клиентом через консоль.
 *  - Реализовать базовые команды (help, clear, exit).
 *  - Вести логирование всех вызовов для отладки.
 *
 * Использование:
 *  Создаётся при запуске клиента (в ApplicationContext).
 *  Команды начинаются с префикса "/" и обрабатываются внутри CommandDefinitions.
 */
public class CommandHandler {

    private static final Logger log = Logger.getLogger(CommandHandler.class.getName());

    /** Объект REPL, обеспечивающий чтение и выполнение команд */
    private final ReplRunner replRunner;

    /**
     * Конструктор инициализирует REPL-среду и регистрирует набор доступных команд.
     *
     * @param ctx контекст приложения
     */
    public CommandHandler(ApplicationContext ctx) {
        replRunner = ReplRunner.builder()
                .withOutput(new PrintStream(System.out, true, StandardCharsets.UTF_8))
                .withCommandFilterPrefix("/") // все команды начинаются с "/"
                .configure(reg -> reg.register(new CommandDefinitions(ctx)))
                .build();
        log.info("CommandHandler initialized");
    }

    /**
     * Возвращает активный экземпляр REPL-раннера.
     *
     * @return объект ReplRunner
     */
    public ReplRunner getReplRunner() {
        return replRunner;
    }

    /**
     * Класс определений команд REPL.
     * Каждая команда оформляется как метод, аннотированный @ReplCommand.
     */
    public static class CommandDefinitions {

        private static final Logger log = Logger.getLogger(CommandDefinitions.class.getName());
        private final ApplicationContext ctx;

        public CommandDefinitions(ApplicationContext ctx) {
            this.ctx = ctx;
        }

        /**
         * Завершает работу клиента.
         * Команда: /exit
         */
        @ReplCommand(name = "/exit", description = "Exit from client")
        public void exit(ReplRunner repl, Arguments args) {
            log.info("Received command /exit");
            repl.println("[SYSTEM] Завершение работы клиента...");
            System.exit(0);
        }

        /**
         * Показывает список доступных команд REPL.
         * Команда: /help
         */
        @ReplCommand(name = "/help", description = "Show help")
        public void help(ReplRunner repl, Arguments args) {
            log.info("Received command /help");
            repl.println(repl.getRegistry().formattedCommandList());
        }

        /**
         * Очищает окно чата.
         * Команда: /clear
         */
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
