package com.thefatrat.application.util;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.util.ArrayList;
import java.util.List;

public class HelpBuilder {

    private final String title;
    private final List<HelpCommand> commands = new ArrayList<>();

    public HelpBuilder(String title, List<Command> commands) {
        this.title = title;
        commands.forEach(command -> {
            if (command.getSubcommands().isEmpty()) {
                this.commands.add(new HelpCommand(command.getName(), command.getDescription()));
                return;
            }
            command.getSubcommands().forEach(sub ->
                this.commands.add(new HelpCommand(
                    command.getName() + " " + sub.getName(), sub.getDescription()))
            );
        });
    }

    public MessageEmbed build(int color) {
        EmbedBuilder builder = new EmbedBuilder()
            .setColor(color)
            .setTitle(title);
        for (HelpCommand command : commands) {
            builder.addField("/" + command.name(), command.description(), false);
        }
        return builder.build();
    }

    private record HelpCommand(String name, String description) {
    }

}
