package com.thefatrat.application.components;

import com.thefatrat.application.DatabaseManager;
import com.thefatrat.application.builders.HelpBuilder;
import com.thefatrat.application.entities.Command;
import com.thefatrat.application.entities.Interaction;
import com.thefatrat.application.exceptions.BotErrorException;
import com.thefatrat.application.exceptions.BotException;
import com.thefatrat.application.handlers.CommandHandler;
import com.thefatrat.application.handlers.MessageInteractionHandler;
import com.thefatrat.application.sources.Server;
import com.thefatrat.application.util.Colors;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.util.*;
import java.util.function.Function;

public abstract class Component {

    private final Server source;
    private final String title;
    private final String name;
    private final boolean alwaysEnabled;
    private boolean enabled;
    private final DatabaseManager databaseManager;
    private final List<Command> commands = new ArrayList<>();
    private final List<Interaction> interactions = new ArrayList<>();
    private final CommandHandler subHandler = new CommandHandler();
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
        CommandHandler handler = getServer().getCommandHandler();

        for (Command command : commands) {
            handler.addListener(command.getName(), command.getAction());

            for (Command sub : command.getSubcommands()) {
                subHandler.addListener(sub.getName(), sub.getAction());
            }
        }

        MessageInteractionHandler messageInteractionHandler = getServer().getInteractionHandler();

        for (Interaction interaction : interactions) {
            messageInteractionHandler.addListener(interaction.getName(), interaction.getAction());
        }

        help = new HelpBuilder(getTitle(), getCommands()).build(Colors.TRANSPARENT);
    }

    public final void setComponentCommand() {
        addCommands(new Command(getName(), "component command")
            .setAction((command, reply) -> getSubCommandHandler().handle(command.toSub(), reply)));
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
    protected CommandHandler getSubCommandHandler() {
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
    public abstract String getStatus();

    public static <T, V> List<T> fillAbsent(Collection<T> expected, Collection<V> actual,
        Function<V, T> keygen, Function<V, T> valgen) {
        Map<T, T> found = new HashMap<>();
        for (V v : actual) {
            found.put(keygen.apply(v), valgen.apply(v));
        }
        List<T> result = new ArrayList<>();
        for (T t : expected) {
            result.add(found.getOrDefault(t, t));
        }
        return result;
    }

}
