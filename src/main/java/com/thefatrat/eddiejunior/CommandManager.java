package com.thefatrat.eddiejunior;

import com.thefatrat.eddiejunior.components.Component;
import com.thefatrat.eddiejunior.entities.Interaction;
import com.thefatrat.eddiejunior.sources.Server;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.IntegrationType;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.internal.requests.CompletedRestAction;
import org.jetbrains.annotations.NotNull;

import javax.annotation.CheckReturnValue;
import java.util.*;

public class CommandManager {

    private final Map<String, Command> retrievedDefaultCommands = new HashMap<>();
    private final Map<String, Map<String, Command>> retrievedGuildCommands = new HashMap<>();
    private final JDA jda;

    public CommandManager(JDA jda) {
        this.jda = jda;
    }

    public void setupGlobalCommands(Class<? extends Component>[] components) {
        retrieveDefaultCommands().complete();

        Server server = Server.dummy();
        Collection<Component> globalInstances = server.registerComponents(components);
        List<com.thefatrat.eddiejunior.entities.Command> commands = new ArrayList<>();
        List<Interaction<?>> messageInteractions = new ArrayList<>();
        List<Interaction<?>> userInteractions = new ArrayList<>();
        for (Component component : globalInstances) {
            commands.addAll(component.getCommands());
            messageInteractions.addAll(component.getMessageInteractions());
            userInteractions.addAll(component.getMemberInteractions());
        }

        List<CommandData> commandDataList = new ArrayList<>();
        Set<String> commandNames = new HashSet<>();
        for (com.thefatrat.eddiejunior.entities.Command command : commands) {
            SlashCommandData slashCommandData = Commands.slash(command.getName(), command.getDescription())
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(command.getPermissions()))
                .addOptions(command.getOptions())
                .addSubcommands(command.getSubcommandsData())
                .setContexts(InteractionContextType.GUILD)
                .setIntegrationTypes(IntegrationType.GUILD_INSTALL);
            commandDataList.add(slashCommandData);
            commandNames.add(command.getName());
        }
        for (Interaction<?> messageInteraction : messageInteractions) {
            CommandData commandData = Commands.message(messageInteraction.getName())
                .setContexts(InteractionContextType.GUILD)
                .setIntegrationTypes(IntegrationType.GUILD_INSTALL);
            commandDataList.add(commandData);
            commandNames.add(messageInteraction.getName());
        }
        for (Interaction<?> userInteraction : userInteractions) {
            CommandData commandData = Commands.user(userInteraction.getName())
                .setContexts(InteractionContextType.GUILD)
                .setIntegrationTypes(IntegrationType.GUILD_INSTALL);
            commandDataList.add(commandData);
            commandNames.add(userInteraction.getName());
        }

        registerDefaultCommands(commandDataList).complete();
        filterDefaultCommands(commandNames).queue();
    }

    public void setupGuildCommands(@NotNull Server server) {
        retrieveGuildCommands(server.getId()).complete();
        filterGuildCommands(server.getId(), server.getRegisteredCommands()).queue();
    }

    public RestAction<?> registerGuildCommands(String server, @NotNull Component component) {
        List<CommandData> commandData = new ArrayList<>();

        for (com.thefatrat.eddiejunior.entities.Command command : component.getCommands()) {
            commandData.add(Commands.slash(command.getName(), command.getDescription())
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(command.getPermissions()))
                .addOptions(command.getOptions())
                .addSubcommands(command.getSubcommandsData())
                .setContexts(InteractionContextType.GUILD)
                .setIntegrationTypes(IntegrationType.GUILD_INSTALL));
        }

        for (Interaction<Message> interaction : component.getMessageInteractions()) {
            commandData.add(Commands.message(interaction.getName())
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(interaction.getPermissions()))
                .setContexts(InteractionContextType.GUILD)
                .setIntegrationTypes(IntegrationType.GUILD_INSTALL));
        }

        for (Interaction<Member> interaction : component.getMemberInteractions()) {
            commandData.add(Commands.user(interaction.getName())
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(interaction.getPermissions()))
                .setContexts(InteractionContextType.GUILD)
                .setIntegrationTypes(IntegrationType.GUILD_INSTALL));
        }

        return registerGuildCommands(server, commandData);
    }

    @CheckReturnValue
    private @NotNull RestAction<Void> filterDefaultCommands(Collection<String> commands) {
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

    public RestAction<?> deregisterGuildCommands(String guildId, @NotNull Component component) {
        List<RestAction<Void>> actions = new ArrayList<>();

        for (com.thefatrat.eddiejunior.entities.Command command : component.getCommands()) {
            actions.add(removeGuildCommand(guildId, command.getName()));
        }

        for (Interaction<Message> interaction : component.getMessageInteractions()) {
            actions.add(removeGuildCommand(guildId, interaction.getName()));
        }

        for (Interaction<Member> interaction : component.getMemberInteractions()) {
            actions.add(removeGuildCommand(guildId, interaction.getName()));
        }

        if (actions.isEmpty()) {
            return new CompletedRestAction<>(jda, (Object) null);
        }

        return RestAction.allOf(actions);
    }

    @CheckReturnValue
    private @NotNull RestAction<Void> filterGuildCommands(String id, Collection<String> commands) {
        List<RestAction<Void>> list = new ArrayList<>();
        Map<String, Command> map = retrievedGuildCommands.get(id);
        Guild guild = jda.getGuildById(id);
        if (map == null || map.isEmpty() || guild == null) {
            return new CompletedRestAction<>(jda, null);
        }
        for (Map.Entry<String, Command> command : map.entrySet()) {
            if (commands.contains(command.getKey())) {
                continue;
            }
            list.add(guild.deleteCommandById(command.getValue().getId())
                .onSuccess(v -> retrievedGuildCommands.get(id).remove(command.getKey()))
            );
        }
        if (list.isEmpty()) {
            return new CompletedRestAction<>(jda, null);
        }
        return RestAction.allOf(list).map(l -> null);
    }

    @CheckReturnValue
    private @NotNull RestAction<List<Command>> retrieveDefaultCommands() {
        return jda.retrieveCommands()
            .onSuccess(this::addRetrievedDefaultCommands);
    }

    private void addRetrievedDefaultCommands(@NotNull Collection<Command> commands) {
        for (Command command : commands) {
            addRetrievedDefaultCommand(command);
        }
    }

    private void addRetrievedDefaultCommand(Command command) {
        retrievedDefaultCommands.put(command.getName(), command);
    }

    @CheckReturnValue
    private @NotNull RestAction<List<Command>> registerDefaultCommands(
        @NotNull List<? extends CommandData> commandData) {

        List<RestAction<Command>> actions = new ArrayList<>();
        for (CommandData command : commandData) {
            if (retrievedDefaultCommands.containsKey(command.getName())) {
                continue;
            }
            actions.add(jda.upsertCommand(command).onSuccess(this::addRetrievedDefaultCommand));
        }
        return actions.isEmpty() ? new CompletedRestAction<>(jda, List.of()) : RestAction.allOf(actions);
    }

    @CheckReturnValue
    private @NotNull RestAction<List<Command>> retrieveGuildCommands(String id) {
        Guild guild = Objects.requireNonNull(jda.getGuildById(id));
        return guild.retrieveCommands()
            .onSuccess(list -> addRetrievedServerCommands(id, list));
    }

    private void addRetrievedServerCommands(String id, @NotNull Collection<Command> commands) {
        for (Command command : commands) {
            addRetrievedServerCommand(id, command);
        }
    }

    private void addRetrievedServerCommand(String id, Command command) {
        retrievedGuildCommands.computeIfAbsent(id, k -> new HashMap<>()).put(command.getName(), command);
    }

    @CheckReturnValue
    private @NotNull RestAction<List<Command>> registerGuildCommands(String guildId,
        @NotNull List<? extends CommandData> commandData) {

        Map<String, Command> map = retrievedGuildCommands.computeIfAbsent(guildId, k -> new HashMap<>());
        Guild guild = Objects.requireNonNull(jda.getGuildById(guildId));
        List<RestAction<Command>> actions = new ArrayList<>();
        for (CommandData command : commandData) {
            if (map.containsKey(command.getName())) {
                break;
            }
            actions.add(guild.upsertCommand(command).onSuccess(c -> this.addRetrievedServerCommand(guildId, c)));
        }
        return actions.isEmpty() ? new CompletedRestAction<>(jda, Collections.emptyList()) : RestAction.allOf(actions);
    }

    @CheckReturnValue
    private @NotNull RestAction<Void> removeGuildCommand(String id, String name) {
        Guild guild = Objects.requireNonNull(jda.getGuildById(id));
        Command command = retrievedGuildCommands.computeIfAbsent(id, k -> new HashMap<>()).get(name);
        if (command == null) {
            return new CompletedRestAction<>(jda, null);
        }
        return guild.deleteCommandById(command.getId()).onSuccess(c -> retrievedGuildCommands.get(id).remove(name));
    }

}
