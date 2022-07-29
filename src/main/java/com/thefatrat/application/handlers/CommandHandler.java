package com.thefatrat.application.handlers;

import com.thefatrat.application.Command;

public class CommandHandler extends ListenerHandler<Command> {

    @Override
    protected void register() {
        addListener("ping", (command) -> {
            final long start = System.currentTimeMillis();
            command.event().getChannel()
                .sendMessage("pong :ping_pong:")
                .queue(message -> {
                    long time = System.currentTimeMillis() - start;
                    message.editMessageFormat("%s %d ms", message.getContentRaw(), time).queue();
                });
        });
    }

    @Override
    public boolean handle(Command command) {
        return execute(command.command(), command);
    }

}
