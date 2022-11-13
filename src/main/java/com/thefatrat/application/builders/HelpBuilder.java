package com.thefatrat.application.builders;

import com.thefatrat.application.entities.Command;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.util.ArrayList;
import java.util.List;

public class HelpBuilder {

    private final String component;
    private final List<String[]> commands = new ArrayList<>();

    public HelpBuilder(String component, List<Command> commands) {
        this.component = component;
        for (Command command : commands) {
            if (command.getSubcommands().isEmpty()) {
                this.commands.add(new String[]{command.getName(), command.getDescription()});
                continue;
            }
            for (Command sub : command.getSubcommands()) {
                this.commands.add(new String[]{command.getName() + " " + sub.getName(), sub.getDescription()});
            }
        }
    }

    public MessageEmbed build(int color) {
        EmbedBuilder builder = new EmbedBuilder()
            .setColor(color)
            .setFooter(component);
        for (String[] command : commands) {
            builder.addField("/" + command[0], command[1], false);
        }
        return builder.build();
    }

}
