package com.thefatrat.eddiejunior.builders;

import com.thefatrat.eddiejunior.components.Component;
import com.thefatrat.eddiejunior.entities.Command;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Used to automatically create a help message for components.
 */
public class HelpMessageBuilder {

    private final String component;
    private final List<MessageEmbed.Field> commands = new ArrayList<>();

    /**
     * Returns a new {@link HelpMessageBuilder} for the given component.
     *
     * @param component the component
     */
    public HelpMessageBuilder(@NotNull Component component) {
        this(component.getId(), component.getCommands());
    }

    /**
     * Returns a new {@link HelpMessageBuilder} for the given component name and commands.
     *
     * @param component the component name
     * @param commands  list of commands
     */
    public HelpMessageBuilder(String component, @NotNull Collection<Command> commands) {
        this.component = component;

        for (Command command : commands) {
            if (command.hasSubCommands()) {
                for (Command sub : command.getSubcommands()) {
                    String commandOptions = formatOptions(sub.getOptions());
                    String commandString = "/" + command.getName() + " " + sub.getName() + " " + commandOptions;
                    this.commands.add(new MessageEmbed.Field(commandString, sub.getDescription(), false));
                }
            } else {
                String commandOptions = formatOptions(command.getOptions());
                String commandString = "/" + command.getName() + " " + commandOptions;
                this.commands.add(new MessageEmbed.Field(commandString, command.getDescription(), false));
            }
        }
    }

    private String formatOptions(@NotNull Collection<OptionData> options) {
        return options.stream()
            .map(this::formatOption)
            .reduce("", (s, o) -> s + " " + o);
    }

    private @NotNull String formatOption(@NotNull OptionData option) {
        if (!option.isRequired()) {
            return "[" + option.getName() + "]";
        } else {
            return "<" + option.getName() + ">";
        }
    }

    /**
     * Creates a new {@link MessageEmbed} with a field for each command.
     *
     * @param color the color of the embed
     * @return a new {@link MessageEmbed}
     */
    public MessageEmbed build(int color) {
        EmbedBuilder builder = new EmbedBuilder()
            .setColor(color)
            .setTitle(component + " help");
        commands.forEach(builder::addField);
        return builder.build();
    }

}
