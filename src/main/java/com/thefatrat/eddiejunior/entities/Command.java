package com.thefatrat.eddiejunior.entities;

import com.thefatrat.eddiejunior.events.CommandEvent;
import com.thefatrat.eddiejunior.reply.InteractionReply;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.jetbrains.annotations.Contract;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;

public class Command implements UserRoleEntity<Command> {

    private final String name;
    private final String description;
    private BiConsumer<CommandEvent, InteractionReply> action = (c, r) -> {};
    private final List<OptionData> options = new ArrayList<>();
    private final List<Command> subcommands = new ArrayList<>();
    private final List<Permission> permissions = new ArrayList<>();
    private UserRole userRole = null;

    public Command(String name, String description) {
        this.name = name;
        this.description = description;
        permissions.add(Permission.USE_APPLICATION_COMMANDS);
    }

    @Override
    public Command setRequiredUserRole(UserRole userRole) {
        this.userRole = userRole;
        return this;
    }

    @Contract("_ -> this")
    public Command addSubcommand(Command command) {
        subcommands.add(command);
        return this;
    }

    @Contract("_ -> this")
    public Command setAction(BiConsumer<CommandEvent, InteractionReply> action) {
        this.action = action;
        return this;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    @Contract("_ -> this")
    public Command addOptions(OptionData... options) {
        Collections.addAll(this.options, options);
        return this;
    }

    @Contract("_ -> this")
    public Command addPermissions(Permission... permissions) {
        Collections.addAll(this.permissions, permissions);
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

    public BiConsumer<CommandEvent, InteractionReply> getAction() {
        return action;
    }

    public List<Permission> getPermissions() {
        return permissions;
    }

    public boolean hasSubCommands() {
        return !subcommands.isEmpty();
    }

    @Override
    public UserRole getRequiredUserRole() {
        return userRole;
    }

}
