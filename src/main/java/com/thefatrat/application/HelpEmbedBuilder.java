package com.thefatrat.application;

import com.thefatrat.application.components.Component;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.util.ArrayList;
import java.util.List;

public class HelpEmbedBuilder {

    private final String title;
    private final List<Command> commands = new ArrayList<>();

    public HelpEmbedBuilder(String title) {
        this.title = title;
    }

    public HelpEmbedBuilder addCommand(String command, String description) {
        commands.add(new Command(command, description));
        return this;
    }

    public MessageEmbed build() {
        return build(Component.getRandomColor(title));
    }

    public MessageEmbed build(int color) {
        EmbedBuilder builder = new EmbedBuilder()
            .setColor(color)
            .setTitle(title);
        for (Command command : commands) {
            builder.addField(command.command(), command.description(), false);
        }
        return builder.build();
    }

    private record Command(String command, String description) {
    }

}
