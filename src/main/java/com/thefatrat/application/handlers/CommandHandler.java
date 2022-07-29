package com.thefatrat.application.handlers;

import com.thefatrat.application.Bot;
import com.thefatrat.application.Command;

public class CommandHandler extends EventHandler<Command> {

    @Override
    protected void register() {
        addListener("ping", command -> {
            final long start = System.currentTimeMillis();
            command.event().getChannel()
                .sendMessage("pong :ping_pong:")
                .queue(message -> {
                    long time = System.currentTimeMillis() - start;
                    message.editMessageFormat("%s %d ms", message.getContentRaw(), time).queue();
                });
        });

        addListener("setprefix", command -> {
            if (command.args().length != 1 || !command.event().isFromGuild()) {
                return;
            }
            Bot.setPrefix(command.event().getGuild().getId(), command.args()[0]);
        });
    }

    @Override
    public boolean handle(Command command) {
        return execute(command.command(), command);
    }

}
