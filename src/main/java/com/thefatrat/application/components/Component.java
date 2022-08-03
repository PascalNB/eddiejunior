package com.thefatrat.application.components;

import com.thefatrat.application.exceptions.BotErrorException;
import com.thefatrat.application.exceptions.BotException;
import com.thefatrat.application.handlers.CommandHandler;
import com.thefatrat.application.sources.Server;
import com.thefatrat.application.util.Command;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public abstract class Component {

    private final Server source;
    private final String title;
    private final boolean alwaysEnabled;
    private final int color;
    private boolean enabled;
    private final List<Command> commands = new ArrayList<>();
    private final CommandHandler subHandler = new CommandHandler();

    public Component(Server source, String title, boolean alwaysEnabled) {
        this.source = source;
        this.title = title;
        this.alwaysEnabled = alwaysEnabled;
        enabled = alwaysEnabled;
        color = getRandomColor(title);
    }

    protected void componentNotFound(String component) throws BotException {
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

    public void register() {
        CommandHandler handler = getServer().getCommandHandler();

        for (Command command : commands) {
            handler.addListener(command.getName(), command.getAction());

            for (Command sub : command.getSubcommands()) {
                subHandler.addListener(sub.getName(), sub.getAction());
            }
        }
    }

    protected CommandHandler getSubHandler() {
        return subHandler;
    }

    public abstract String getStatus();

    public int getColor() {
        return color;
    }

    public List<Command> getCommands() {
        return commands;
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

    public static int getRandomColor(String seed) {
        int hash = 0;
        for (int i = 0; i < seed.length(); i++) {
            hash = seed.charAt(i) + ((hash << 5) - hash);
        }
        return hash & 0x00FFFFFF;
    }

    public <T> String concatObjects(T[] objects, Function<T, String> toString) {
        StringBuilder builder = new StringBuilder();
        for (T o : objects) {
            builder.append(toString.apply(o)).append(", ");
        }
        builder.delete(builder.length() - 2, builder.length());
        return builder.toString();
    }

}
