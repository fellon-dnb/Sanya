package com.sanya;

import com.ancevt.replines.core.argument.Arguments;
import com.sanya.commands.CommandHandler;
import com.sanya.events.*;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * Консольная версия клиента (без GUI).
 * Поддерживает EventBus и команды /help, /exit, /clear.
 */
public class ChatClientConsole {

    private final EventBus eventBus = new SimpleEventBus();
    private final ChatClientConnector connector;
    private final CommandHandler commandHandler;
    private final Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8);

    public ChatClientConsole(String host, int port, String name) {
        connector = new ChatClientConnector(host, port, name, eventBus);
        connector.connect();

        // подписки на события
        eventBus.subscribe(MessageReceivedEvent.class, e -> System.out.println(e.message()));
        eventBus.subscribe(ClearChatEvent.class, e -> clearConsole());

        // создаём обработчик команд (вывод идёт в консоль)
        commandHandler = new CommandHandler(eventBus);

        System.out.println("[SYSTEM] Подключение успешно. Введите сообщение или /help.");
    }

    private void clearConsole() {
        // Очистка консоли (универсальная кроссплатформенная попытка)
        try {
            if (System.getProperty("os.name").contains("Windows")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                System.out.print("\033[H\033[2J");
                System.out.flush();
            }
        } catch (Exception ignored) {}
    }

    public void start() {
        while (true) {
            String text = scanner.nextLine().trim();
            if (text.isEmpty()) continue;

            if (text.startsWith("/")) {
                try {
                    commandHandler.getReplRunner().execute(text);
                } catch (com.ancevt.replines.core.repl.UnknownCommandException e) {
                    System.out.println("[SYSTEM] Неизвестная команда: " + text);
                }
            } else {
                eventBus.publish(new MessageSendEvent(text));
            }

        }
    }

    public static void main(String[] args) {
        System.setProperty("file.encoding", "UTF-8");
        System.setProperty("sun.stdout.encoding", "UTF-8");
        System.setProperty("sun.stderr.encoding", "UTF-8");
        System.out.println("[DEBUG] Encoding forced to UTF-8");
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8));
        Arguments a = Arguments.parse(args);
        String host = a.get(String.class, "--host", "localhost");
        int port = a.get(Integer.class, "--port", 12345);

        System.out.print("Enter your Name: ");
        Scanner sc = new Scanner(System.in, StandardCharsets.UTF_8);
        String name = sc.nextLine();

        new ChatClientConsole(host, port, name).start();
    }

    /**
     * Простая заглушка вместо JTextArea — направляет вывод в System.out.
     */
    private static class JTextAreaProxy extends javax.swing.JTextArea {
        @Override
        public void append(String str) {
            System.out.print(str);
        }
    }
}
