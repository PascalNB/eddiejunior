package com.thefatrat.application.builders;

import com.thefatrat.application.entities.Command;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.ArrayList;
import java.util.List;

public class HelpBuilder {

    private final String component;
    private final List<String[]> commands = new ArrayList<>();

    public HelpBuilder(String component, List<Command> commands) {
        this.component = component;
        for (Command command : commands) {
            if (command.getSubcommands().isEmpty()) {
                StringBuilder builder = new StringBuilder();
                for (OptionData option : command.getOptions()) {
                    if (!option.isRequired()) {
                        builder.append(" [").append(option.getName()).append("]");
                    } else {
                        builder.append(" <").append(option.getName()).append(">");
                    }
                }
                this.commands.add(new String[]{command.getName(), command.getDescription(), builder.toString()});
                continue;
            }
            for (Command sub : command.getSubcommands()) {
                StringBuilder builder = new StringBuilder();
                for (OptionData option : sub.getOptions()) {
                    if (!option.isRequired()) {
                        builder.append(" [").append(option.getName()).append("]");
                    } else {
                        builder.append(" <").append(option.getName()).append(">");
                    }
                }
                this.commands.add(new String[]{
                    command.getName() + " " + sub.getName(), sub.getDescription(), builder.toString()});
            }
        }
    }

    public MessageEmbed build(int color) {
        EmbedBuilder builder = new EmbedBuilder()
            .setColor(color)
            .setTitle(component + " help");
        for (String[] command : commands) {
            builder.addField("/" + command[0] + command[2], command[1], false);
        }
        return builder.build();
    }

}
