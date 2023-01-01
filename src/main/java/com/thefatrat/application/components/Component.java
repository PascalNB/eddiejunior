package com.thefatrat.application.components;

import com.thefatrat.application.DatabaseManager;
import com.thefatrat.application.builders.HelpBuilder;
import com.thefatrat.application.entities.Command;
import com.thefatrat.application.entities.Interaction;
import com.thefatrat.application.events.CommandEvent;
import com.thefatrat.application.events.MessageInteractionEvent;
import com.thefatrat.application.exceptions.BotErrorException;
import com.thefatrat.application.exceptions.BotException;
import com.thefatrat.application.handlers.MapHandler;
import com.thefatrat.application.reply.Reply;
import com.thefatrat.application.sources.Server;
import com.thefatrat.application.util.Colors;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public abstract class Component {

    private final Server source;
    private final String title;
    private final String name;
    private final boolean alwaysEnabled;
    private boolean enabled;
    private final DatabaseManager databaseManager;
    private final List<Command> commands = new ArrayList<>();
    private final List<Interaction> interactions = new ArrayList<>();
    private final MapHandler<CommandEvent, Reply> subHandler = new MapHandler<>();
    private MessageEmbed help = null;

    /**
     * Constructs a new component for the given server and with the given title.
     *
     * @param server        the server
     * @param title         the component title
     * @param alwaysEnabled whether the component should always be enabled
     */
    public Component(Server server, String title, boolean alwaysEnabled) {
        this.source = server;
        this.title = title;
        this.name = title.toLowerCase(Locale.ROOT);
        this.alwaysEnabled = alwaysEnabled;
        enabled = alwaysEnabled;
        databaseManager = new DatabaseManager(server.getId(), getName());
    }

    /**
     * Throws specific {@link BotErrorException} with the given component name.
     *
     * @param component the component name
     * @throws BotException always
     */
    protected final void componentNotFound(String component) throws BotException {
        throw new BotErrorException(String.format("Component `%s` does not exist", component));
    }

    /**
     * @return whether the component is always enabled and cannot be disabled
     */
    public boolean isAlwaysEnabled() {
        return alwaysEnabled;
    }

    /**
     * Enables the component
     */
    public void enable() {
        enabled = true;
    }

    /**
     * Disables the component
     */
    public void disable() {
        enabled = alwaysEnabled;
    }

    /**
     * @return whether the component is enabled
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * @return the server which the component belongs to
     */
    public Server getServer() {
        return source;
    }

    /**
     * @return the title of the component
     */
    public String getTitle() {
        return title;
    }

    /**
     * Returns a lowercase version of the component's title.
     *
     * @return the name of the component
     */
    public String getName() {
        return name;
    }

    /**
     * Will register all the commands and interactions to the server.
     */
    public final void register() {
        MapHandler<CommandEvent, Reply> handler = getServer().getCommandHandler();

        for (Command command : commands) {
            handler.addListener(command.getName(), command.getAction());

            for (Command sub : command.getSubcommands()) {
                subHandler.addListener(sub.getName(), sub.getAction());
            }
        }

        MapHandler<MessageInteractionEvent, Reply> messageInteractionHandler = getServer().getInteractionHandler();

        for (Interaction interaction : interactions) {
            messageInteractionHandler.addListener(interaction.getName(), interaction.getAction());
        }

        help = new HelpBuilder(getTitle(), getCommands()).build(Colors.TRANSPARENT);
    }

    protected final void setComponentCommand(Permission... permissions) {
        commands.add(new Command(getName(), "component command")
            .addPermissions(permissions)
            .setAction((command, reply) -> {
                CommandEvent event = command.toSub();
                getSubCommandHandler().handle(event.getCommand(), event, reply);
            })
        );
    }

    /**
     * Returns an embed with a list of all the component's commands
     *
     * @return the component's help message
     */
    public MessageEmbed getHelp() {
        return help;
    }

    /**
     * @return the component's subcommand handler
     */
    protected MapHandler<CommandEvent, Reply> getSubCommandHandler() {
        return subHandler;
    }

    /**
     * @return a list of the component's commands
     */
    public List<Command> getCommands() {
        return commands;
    }

    /**
     * @return a list of the component's interactions
     */
    public List<Interaction> getInteractions() {
        return interactions;
    }

    /**
     * Adds the given commands to the component's list of commands
     *
     * @param commands the commands
     */
    protected void addCommands(Command... commands) {
        this.commands.addAll(List.of(commands));
    }

    /**
     * Adds the given subcommands to the component's list of subcommands.
     * The subcommands will be part of the main command that is identical to the component's name.
     *
     * @param subcommands the subcommands
     */
    protected void addSubcommands(Command... subcommands) {
        for (Command c : commands) {
            if (Objects.equals(c.getName(), getName())) {
                for (Command s : subcommands) {
                    subHandler.addListener(s.getName(), s.getAction());
                    c.addSubcommand(s);
                }
                break;
            }
        }
    }

    /**
     * Adds the given interactions to the component's list of interactions.
     *
     * @param interactions the interactions
     */
    protected void addInteractions(Interaction... interactions) {
        for (Interaction i : interactions) {
            i.setName(getName() + " " + i.getName());
        }
        this.interactions.addAll(List.of(interactions));
    }

    /**
     * @return the component's database manager
     */
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    /**
     * @return the current status of the component
     */
    public String getStatus() {
        return String.format("""
                Enabled: %b
                """,
            isEnabled());
    }

}
