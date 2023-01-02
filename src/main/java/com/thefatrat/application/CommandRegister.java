package com.thefatrat.application;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.internal.requests.CompletedRestAction;

import javax.annotation.CheckReturnValue;
import java.util.*;

public class CommandRegister {

    private final Map<String, Command> retrievedDefaultCommands = new HashMap<>();
    private final Map<String, Map<String, Command>> retrievedServerCommands = new HashMap<>();
    private final JDA jda;

    public CommandRegister(JDA jda) {
        this.jda = jda;
    }

    @CheckReturnValue
    public RestAction<Void> filterDefaultCommands(Collection<String> commands) {
        List<RestAction<Void>> list = new ArrayList<>();
        for (Map.Entry<String, Command> command : retrievedDefaultCommands.entrySet()) {
            if (commands.contains(command.getKey())) {
                continue;
            }
            list.add(jda.deleteCommandById(command.getValue().getId())
                .onSuccess(v -> retrievedDefaultCommands.remove(command.getKey()))
            );
        }
        if (list.isEmpty()) {
            return new CompletedRestAction<>(jda, null);
        }
        return RestAction.allOf(list).map(l -> null);
    }

    @CheckReturnValue
    public RestAction<Void> filterServerCommands(String id, Collection<String> commands) {
        List<RestAction<Void>> list = new ArrayList<>();
        Map<String, Command> map = retrievedServerCommands.get(id);
        Guild guild = jda.getGuildById(id);
        if (map == null || map.isEmpty() || guild == null) {
            return new CompletedRestAction<>(jda, null);
        }
        for (Map.Entry<String, Command> command : map.entrySet()) {
            if (commands.contains(command.getKey())) {
                continue;
            }
            list.add(guild.deleteCommandById(command.getValue().getId())
                .onSuccess(v -> retrievedServerCommands.get(id).remove(command.getKey()))
            );
        }
        if (list.isEmpty()) {
            return new CompletedRestAction<>(jda, null);
        }
        return RestAction.allOf(list).map(l -> null);
    }

    @CheckReturnValue
    public RestAction<List<Command>> retrieveDefaultCommands() {
        return jda.retrieveCommands()
            .onSuccess(this::addRetrievedDefaultCommands);
    }

    private void addRetrievedDefaultCommands(Collection<Command> commands) {
        for (Command command : commands) {
            addRetrievedDefaultCommand(command);
        }
    }

    private void addRetrievedDefaultCommand(Command command) {
        retrievedDefaultCommands.put(command.getName(), command);
    }

    @CheckReturnValue
    public RestAction<List<Command>> registerDefaultCommands(List<? extends CommandData> commandData) {
        List<RestAction<Command>> actions = new ArrayList<>();
        for (CommandData command : commandData) {
            if (retrievedDefaultCommands.containsKey(command.getName())) {
                continue;
            }
            actions.add(jda.upsertCommand(command).onSuccess(this::addRetrievedDefaultCommand));
        }
        return actions.size() == 0 ? new CompletedRestAction<>(jda, List.of()) : RestAction.allOf(actions);
    }

    @CheckReturnValue
    public RestAction<List<Command>> retrieveServerCommands(String id) {
        Guild guild = Objects.requireNonNull(jda.getGuildById(id));
        return guild.retrieveCommands()
            .onSuccess(list -> addRetrievedServerCommands(id, list));
    }

    private void addRetrievedServerCommands(String id, Collection<Command> commands) {
        for (Command command : commands) {
            addRetrievedServerCommand(id, command);
        }
    }

    private void addRetrievedServerCommand(String id, Command command) {
        retrievedServerCommands.computeIfAbsent(id, k -> new HashMap<>()).put(command.getName(), command);
    }

    @CheckReturnValue
    public RestAction<List<Command>> registerServerCommands(String id, List<? extends CommandData> commandData) {
        Map<String, Command> map = retrievedServerCommands.computeIfAbsent(id, k -> new HashMap<>());
        Guild guild = Objects.requireNonNull(jda.getGuildById(id));
        List<RestAction<Command>> actions = new ArrayList<>();
        for (CommandData command : commandData) {
            if (map.containsKey(command.getName())) {
                break;
            }
            actions.add(guild.upsertCommand(command).onSuccess(c -> this.addRetrievedServerCommand(id, c)));
        }
        return actions.size() == 0 ? new CompletedRestAction<>(jda, List.of()) : RestAction.allOf(actions);
    }

    @CheckReturnValue
    public RestAction<Void> removeServerCommand(String id, String name) {
        Guild guild = Objects.requireNonNull(jda.getGuildById(id));
        Command command = retrievedServerCommands.computeIfAbsent(id, k -> new HashMap<>()).get(name);
        if (command == null) {
            return new CompletedRestAction<>(jda, null);
        }
        return guild.deleteCommandById(command.getId()).onSuccess(c -> retrievedServerCommands.get(id).remove(name));
    }

}
