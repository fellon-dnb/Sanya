package com.sanya.client;

import com.ancevt.replines.core.argument.Arguments;
import com.sanya.events.*;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class ChatClientConsole {

    private final ChatClientConnector connector;
    private final Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8);
    private final ApplicationContext ctx;

    public ChatClientConsole(String host, int port, String username) {
        ctx = new ApplicationContext(new ConnectionInfo(host, port));
        ctx.getUserInfo().setName(username);

        connector = new ChatClientConnector(host, port, username, ctx.getEventBus());
        connector.connect();

        ctx.getEventBus().subscribe(MessageReceivedEvent.class, e -> System.out.println(e.message()));
        ctx.getEventBus().subscribe(ClearChatEvent.class, e -> clearConsole());

        System.out.println("[SYSTEM] Подключение успешно. Введите сообщение или /help.");
    }

    private void clearConsole() {
        try {
            if (System.getProperty("os.name").contains("Windows")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                System.out.print("\033[H\033[2J"); System.out.flush();
            }
        } catch (Exception ignored) {}
    }

    public void start() {
        while (true) {
            String text = scanner.nextLine().trim();
            if (text.isEmpty()) continue;

            if (text.startsWith("/")) {
                try {
                    ctx.getCommandHandler().getReplRunner().execute(text);
                } catch (com.ancevt.replines.core.repl.UnknownCommandException e) {
                    System.out.println("[SYSTEM] Неизвестная команда: " + text);
                }
            } else {
                ctx.getEventBus().publish(new MessageSendEvent(text));
            }
        }
    }

    public static void main(String[] args) {
        System.setProperty("file.encoding", "UTF-8");
        System.setProperty("sun.stdout.encoding", "UTF-8");
        System.setProperty("sun.stderr.encoding", "UTF-8");
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
}
