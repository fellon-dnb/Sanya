package com.sanya.commands;

import com.ancevt.replines.core.repl.ReplRunner;

public class CommandHandler {


    private final ReplRunner replRunner;

    // TODO: pass context
    public CommandHandler() {
        this.replRunner = ReplRunner.builder()
                .withColorizer()
                .configure(reg -> {
                    reg.command("/exit")
                            .action((r, args) -> {
                                //TODO: implement exitting by events
                                System.exit(0);
                            })
                            .build();
                    reg.command("/help")
                            .action((r, args) -> {
                                //TODO: print help list
                                r.println("//TODO: print help list");
                            })
                            .build();
                })
                .build();
    }

    public ReplRunner getReplRunner() {
        return replRunner;
    }
}
