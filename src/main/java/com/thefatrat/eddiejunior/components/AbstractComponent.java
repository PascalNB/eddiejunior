package com.thefatrat.eddiejunior.components;

import com.pascalnb.dbwrapper.StringMapper;
import com.thefatrat.eddiejunior.DatabaseManager;
import com.thefatrat.eddiejunior.entities.Command;
import com.thefatrat.eddiejunior.entities.Interaction;
import com.thefatrat.eddiejunior.sources.Server;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Abstract implementation of a component.
 */
public abstract class AbstractComponent implements Component {

    private final Server server;
    private final String id;
    private boolean enabled;
    private final DatabaseManager databaseManager;
    private final List<Command> commands = new ArrayList<>();
    private final List<Interaction<Message>> messageInteractions = new ArrayList<>();
    private final List<Interaction<Member>> memberInteractions = new ArrayList<>();
    private Command componentCommand = null;

    /**
     * Constructs a new component for the given server and with the given id.
     *
     * @param server the server
     * @param id     the component id
     */
    public AbstractComponent(@NotNull Server server, @NotNull String id) {
        this.server = server;
        this.id = id.toLowerCase(Locale.ROOT);
        this.enabled = false;
        databaseManager = new DatabaseManager(server.getId(), getId());
    }

    @Override
    public void enable() {
        enabled = true;
    }

    @Override
    public void disable() {
        enabled = false;
    }

    @Override
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * @return the server which the component belongs to
     */
    public Server getServer() {
        return server;
    }

    /**
     * @return the {@link Guild} object of the server
     */
    public Guild getGuild() {
        return server.getGuild();
    }

    @Override
    public String getId() {
        return id;
    }

    /**
     * Adds the component name as a command which allows subcommands to be added.
     * Use {@link AbstractComponent#addSubcommands(Command...)} to add subcommands.
     *
     * @param permissions the default permissions the command requires
     */
    protected final void setComponentCommand(Permission... permissions) {
        if (componentCommand == null) {
            componentCommand = new Command(getId(), "component command")
                .addPermissions(permissions);
            commands.add(componentCommand);
        }
    }

    /**
     * Get all the requested settings from the database as a {@link Map}.
     *
     * @param settings the requested settings
     * @return a map containing each setting and the value as a {@link StringMapper}
     */
    public Map<String, StringMapper> getSettings(String... settings) {
        return getDatabaseManager().getAll(List.of(settings));
    }

    @Override
    public List<Command> getCommands() {
        return commands;
    }

    @Override
    public List<Interaction<Message>> getMessageInteractions() {
        return messageInteractions;
    }

    @Override
    public List<Interaction<Member>> getMemberInteractions() {
        return memberInteractions;
    }

    /**
     * Adds the given commands to the component's list of commands
     *
     * @param commands the commands
     */
    protected void addCommands(Command... commands) {
        this.commands.addAll(Arrays.asList(commands));
    }

    /**
     * Adds the given subcommands to the component's list of subcommands.
     * The subcommands will be part of the main command that is identical to the component's name.
     *
     * @param subcommands the subcommands
     */
    protected void addSubcommands(Command... subcommands) {
        if (componentCommand == null) {
            throw new UnsupportedOperationException("Component command has not been set");
        }
        for (Command s : subcommands) {
            componentCommand.addSubcommand(s);
        }
    }

    /**
     * Adds the given message context interactions to the component's list of interactions.
     *
     * @param interactions the message context interactions
     */
    @SafeVarargs
    protected final void addMessageInteractions(@NotNull Interaction<Message> @NotNull ... interactions) {
        for (Interaction<Message> i : interactions) {
            i.setName(getId() + " " + i.getName());
        }
        this.messageInteractions.addAll(List.of(interactions));
    }

    /**
     * Adds the given user context interactions to the component's list of interactions.
     *
     * @param interactions the user context interactions
     */
    @SafeVarargs
    protected final void addMemberInteractions(@NotNull Interaction<Member> @NotNull ... interactions) {
        for (Interaction<Member> i : interactions) {
            i.setName(getId() + " " + i.getName());
        }
        this.memberInteractions.addAll(List.of(interactions));
    }

    /**
     * @return the component's database manager
     */
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

}
