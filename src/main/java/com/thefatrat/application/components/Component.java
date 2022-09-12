package com.thefatrat.application.components;

import com.thefatrat.application.DatabaseManager;
import com.thefatrat.application.builders.HelpBuilder;
import com.thefatrat.application.entities.Command;
import com.thefatrat.application.entities.Interaction;
import com.thefatrat.application.exceptions.BotErrorException;
import com.thefatrat.application.exceptions.BotException;
import com.thefatrat.application.handlers.CommandHandler;
import com.thefatrat.application.handlers.InteractionHandler;
import com.thefatrat.application.sources.Server;
import com.thefatrat.application.util.Colors;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.util.*;
import java.util.function.Function;

public abstract class Component {

    private final Server source;
    private final String title;
    private final boolean alwaysEnabled;
    private boolean enabled;
    private final DatabaseManager databaseManager;
    private final List<Command> commands = new ArrayList<>();
    private final List<Interaction> interactions = new ArrayList<>();
    private final CommandHandler subHandler = new CommandHandler();
    private MessageEmbed help = null;

    public Component(Server source, String title, boolean alwaysEnabled) {
        this.source = source;
        this.title = title;
        this.alwaysEnabled = alwaysEnabled;
        enabled = alwaysEnabled;
        databaseManager = new DatabaseManager(source.getId(), getName());
    }

    protected final void componentNotFound(String component) throws BotException {
        throw new BotErrorException(String.format("Component `%s` does not exist", component));
    }

    public boolean isAlwaysEnabled() {
        return alwaysEnabled;
    }

    public void enable() {
        enabled = true;
    }

    public void disable() {
        enabled = alwaysEnabled;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isEnabled() {
        return enabled;
    }

    public Server getServer() {
        return source;
    }

    public String getTitle() {
        return title;
    }

    public String getName() {
        return title.toLowerCase();
    }

    public final void register() {
        CommandHandler handler = getServer().getCommandHandler();

        for (Command command : commands) {
            handler.addListener(command.getName(), command.getAction());

            for (Command sub : command.getSubcommands()) {
                subHandler.addListener(sub.getName(), sub.getAction());
            }
        }

        InteractionHandler interactionHandler = getServer().getInteractionHandler();

        for (Interaction interaction : interactions) {
            interactionHandler.addListener(getName(), interaction.getName(), interaction.getAction());
        }

        help = new HelpBuilder(getName(), getCommands()).build(Colors.BLUE);
    }

    public MessageEmbed getHelp() {
        return help;
    }

    protected CommandHandler getSubHandler() {
        return subHandler;
    }

    public abstract String getStatus();

    public List<Command> getCommands() {
        return commands;
    }

    public List<Interaction> getInteractions() {
        return interactions;
    }

    protected void addSubcommands(Command... subcommands) {
        for (Command c : commands) {
            if (Objects.equals(c.getName(), getName())) {
                for (Command s : subcommands) {
                    subHandler.addListener(s.getName(), s.getAction());
                    c.addSubcommand(s);
                }
            }
        }
    }

    protected void addCommands(Command... commands) {
        this.commands.addAll(List.of(commands));
    }

    protected void addInteractions(Interaction... interactions) {
        this.interactions.addAll(List.of(interactions));
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

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

    public static <T> String concatObjects(T[] objects, Function<T, String> toString) {
        if (objects.length == 0) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (T o : objects) {
            builder.append(toString.apply(o)).append(", ");
        }
        builder.delete(builder.length() - 2, builder.length());
        return builder.toString();
    }

}
