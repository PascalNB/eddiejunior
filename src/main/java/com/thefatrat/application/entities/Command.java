package com.thefatrat.application.entities;

import com.thefatrat.application.events.CommandEvent;
import com.thefatrat.application.reply.Reply;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;

public class Command {

    private final String name;
    private final String description;
    private BiConsumer<CommandEvent, Reply> action = (__, ___) -> {};
    private final List<OptionData> options = new ArrayList<>();
    private final List<Command> subcommands = new ArrayList<>();

    public Command(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public Command addSubcommand(Command command) {
        subcommands.add(command);
        return this;
    }

    public Command setAction(BiConsumer<CommandEvent, Reply> action) {
        this.action = action;
        return this;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Command addOptions(OptionData... options) {
        Collections.addAll(this.options, options);
        return this;
    }

    public Command addOption(OptionData option) {
        options.add(option);
        return this;
    }

    public List<OptionData> getOptions() {
        return options;
    }

    public List<Command> getSubcommands() {
        return subcommands;
    }

    public List<SubcommandData> getSubcommandsData() {
        List<SubcommandData> result = new ArrayList<>();

        for (Command command : subcommands) {
            SubcommandData sub = new SubcommandData(command.getName(), command.getDescription())
                .addOptions(command.getOptions());
            result.add(sub);
        }

        return result;
    }

    public BiConsumer<CommandEvent, Reply> getAction() {
        return action;
    }

}
