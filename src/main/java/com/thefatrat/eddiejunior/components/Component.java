package com.thefatrat.eddiejunior.components;

import com.pascalnb.dbwrapper.StringMapper;
import com.thefatrat.eddiejunior.DatabaseManager;
import com.thefatrat.eddiejunior.builders.HelpBuilder;
import com.thefatrat.eddiejunior.entities.Command;
import com.thefatrat.eddiejunior.entities.Interaction;
import com.thefatrat.eddiejunior.events.CommandEvent;
import com.thefatrat.eddiejunior.handlers.MapHandler;
import com.thefatrat.eddiejunior.reply.InteractionReply;
import com.thefatrat.eddiejunior.sources.Server;
import com.thefatrat.eddiejunior.util.Colors;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public abstract class Component {

    private final Server server;
    private final String title;
    private final String name;
    private final boolean globalComponent;
    private boolean enabled;
    private final DatabaseManager databaseManager;
    private final List<Command> commands = new ArrayList<>();
    private final List<Interaction<Message>> messageInteractions = new ArrayList<>();
    private final List<Interaction<Member>> memberInteractions = new ArrayList<>();
    private final MapHandler<CommandEvent, InteractionReply> subHandler = new MapHandler<>();
    private MessageEmbed help = null;
    private boolean isComponentCommand = false;

    /**
     * Constructs a new component for the given server and with the given title.
     *
     * @param server          the server
     * @param title           the component title
     * @param globalComponent whether the component should always be enabled
     */
    public Component(@NotNull Server server, @NotNull String title, boolean globalComponent) {
        this.server = server;
        this.title = title;
        this.name = title.toLowerCase(Locale.ROOT);
        this.globalComponent = globalComponent;
        enabled = globalComponent;
        databaseManager = new DatabaseManager(server.getId(), getName());
    }

    /**
     * @return whether the component is always enabled and cannot be disabled
     */
    public boolean isGlobalComponent() {
        return globalComponent;
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
        enabled = globalComponent;
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
        return server;
    }

    public Guild getGuild() {
        return server.getGuild();
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

        for (Command command : commands) {
            getServer().getCommandHandler().addListener(command.getName(), command.getAction());

            for (Command sub : command.getSubcommands()) {
                getSubCommandHandler().addListener(sub.getName(), sub.getAction());
            }
        }

        for (Interaction<Message> interaction : messageInteractions) {
            getServer().getMessageInteractionHandler().addListener(interaction.getName(), interaction.getAction());
        }

        for (Interaction<Member> interaction : memberInteractions) {
            getServer().getMemberInteractionHandler().addListener(interaction.getName(), interaction.getAction());
        }

        help = new HelpBuilder(getTitle(), getCommands()).build(Colors.TRANSPARENT);
    }

    protected final void setComponentCommand(Permission... permissions) {
        if (!isComponentCommand) {
            commands.add(new Command(getName(), "component command")
                .addPermissions(permissions)
                .setAction((command, reply) -> {
                    CommandEvent event = command.toSub();
                    getSubCommandHandler().handle(event.getCommand(), event, reply);
                })
            );
            isComponentCommand = true;
        }
    }

    public Map<String, StringMapper> getSettings(String... settings) {
        return getDatabaseManager().getAll(List.of(settings));
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
    protected MapHandler<CommandEvent, InteractionReply> getSubCommandHandler() {
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
    public List<Interaction<Message>> getMessageInteractions() {
        return messageInteractions;
    }

    public List<Interaction<Member>> getMemberInteractions() {
        return memberInteractions;
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
        if (!isComponentCommand) {
            return;
        }
        for (Command c : commands) {
            if (Objects.equals(c.getName(), getName())) {
                for (Command s : subcommands) {
                    getSubCommandHandler().addListener(s.getName(), s.getAction());
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
    @SafeVarargs
    protected final void addMessageInteractions(@NotNull Interaction<Message>... interactions) {
        for (Interaction<Message> i : interactions) {
            i.setName(getName() + " " + i.getName());
        }
        this.messageInteractions.addAll(List.of(interactions));
    }

    @SafeVarargs
    protected final void addMemberInteractions(@NotNull Interaction<Member>... interactions) {
        for (Interaction<Member> i : interactions) {
            i.setName(getName() + " " + i.getName());
        }
        this.memberInteractions.addAll(List.of(interactions));
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
