package com.thefatrat.application;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.internal.requests.CompletedRestAction;

import java.util.*;

public class CommandRegister {

    private static final CommandRegister instance = new CommandRegister();

    private final Map<String, Command> defaultCommands = new HashMap<>();
    private final Map<String, Map<String, Command>> serverCommands = new HashMap<>();
    private JDA jda;

    private CommandRegister() {}

    public static CommandRegister getInstance() {
        return instance;
    }

    public void setJDA(JDA jda) {
        this.jda = jda;
    }

    public void retrieveDefaultCommands() {
        for (Command command : jda.retrieveCommands().complete()) {
            addDefaultCommand(command);
        }
    }

    public void addDefaultCommand(Command command) {
        defaultCommands.put(command.getName(), command);
    }

    public RestAction<List<Command>> registerDefaultCommands(List<? extends CommandData> commandData) {
        List<RestAction<Command>> actions = new ArrayList<>();
        for (CommandData command : commandData) {
            if (defaultCommands.containsKey(command.getName())) {
                continue;
            }
            actions.add(jda.upsertCommand(command).onSuccess(this::addDefaultCommand));
        }
        return actions.size() == 0 ? new CompletedRestAction<>(jda, List.of()) : RestAction.allOf(actions);
    }

    public void retrieveServerCommands(String id) {
        Guild guild = Objects.requireNonNull(jda.getGuildById(id));
        for (Command command : guild.retrieveCommands().complete()) {
            addServerCommand(id, command);
        }
    }

    public void addServerCommand(String id, Command command) {
        serverCommands.computeIfAbsent(id, k -> new HashMap<>()).put(command.getName(), command);
    }

    public RestAction<List<Command>> registerServerCommands(String id, List<? extends CommandData> commandData) {
        Map<String, Command> map = serverCommands.computeIfAbsent(id, k -> new HashMap<>());
        Guild guild = Objects.requireNonNull(jda.getGuildById(id));
        List<RestAction<Command>> actions = new ArrayList<>();
        for (CommandData command : commandData) {
            if (map.containsKey(command.getName())) {
                break;
            }
            actions.add(guild.upsertCommand(command).onSuccess(c -> this.addServerCommand(id, c)));
        }
        return actions.size() == 0 ? new CompletedRestAction<>(jda, List.of()) : RestAction.allOf(actions);
    }

    public RestAction<Void> removeServerCommand(String id, String name) {
        Guild guild = Objects.requireNonNull(jda.getGuildById(id));
        Command command = serverCommands.computeIfAbsent(id, k -> new HashMap<>()).get(name);
        if (command == null) {
            return new CompletedRestAction<>(jda, null);
        }
        return guild.deleteCommandById(command.getId()).onSuccess(c -> serverCommands.get(id).remove(name));
    }

}
