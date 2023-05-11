package com.thefatrat.eddiejunior.components;

import com.thefatrat.eddiejunior.entities.Command;
import com.thefatrat.eddiejunior.events.CommandEvent;
import com.thefatrat.eddiejunior.exceptions.BotErrorException;
import com.thefatrat.eddiejunior.exceptions.BotWarningException;
import com.thefatrat.eddiejunior.reply.InteractionReply;
import com.thefatrat.eddiejunior.reply.MenuReply;
import com.thefatrat.eddiejunior.reply.Reply;
import com.thefatrat.eddiejunior.sources.Server;
import com.thefatrat.eddiejunior.util.Colors;
import com.thefatrat.eddiejunior.util.Icon;
import com.thefatrat.eddiejunior.util.PermissionChecker;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.IMentionable;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;

public abstract class DirectMessageComponent extends Component implements RunnableComponent {

    private final String alt;
    private final boolean autoRun;
    private final Set<String> blacklist = new HashSet<>();
    private boolean running = false;
    private String destination;

    public DirectMessageComponent(Server server, String title, String alt, boolean autoRun) {
        super(server, title, false);
        this.alt = alt;
        this.autoRun = autoRun;
        destination = getDatabaseManager().getSetting("destination");
        if (autoRun && getDatabaseManager().getSettingOrDefault("running", false)) {
            start(Reply.EMPTY);
        }
        blacklist.addAll(getDatabaseManager().getSettings("blacklist"));

        setComponentCommand();

        addSubcommands(
            new Command("start", "starts the component")
                .addOptions(new OptionData(OptionType.CHANNEL, "channel", "channel destination")
                    .setChannelTypes(ChannelType.TEXT)
                )
                .setAction(this::startCommand),

            new Command("stop", "stops the component")
                .setAction(this::stopCommand),

            new Command("destination", "sets the destination channel")
                .addOptions(new OptionData(OptionType.CHANNEL, "channel", "destination channel", false)
                    .setChannelTypes(ChannelType.TEXT)
                )
                .setAction(this::setDestination),

            new Command("blacklist", "manages the blacklist")
                .addOptions(new OptionData(OptionType.STRING, "action", "action", true)
                    .addChoice("add", "add")
                    .addChoice("remove", "remove")
                    .addChoice("show", "show")
                    .addChoice("clear", "clear")
                )
                .addOptions(new OptionData(OptionType.USER, "user", "user", false))
                .setAction(this::blacklistCommand)
        );
    }

    private void startCommand(CommandEvent command, InteractionReply reply) {
        OptionMapping option = command.get("channel");
        TextChannel parsedDestination = option == null ? null : option.getAsChannel().asTextChannel();
        TextChannel newDestination;
        TextChannel currentDestination = getDestination();

        if (currentDestination == null && parsedDestination == null) {
            try {
                newDestination = command.getChannel().asTextChannel();
            } catch (IllegalStateException e) {
                throw new BotErrorException("Can only start in a text channel");
            }
        } else {
            if (parsedDestination != null) {
                newDestination = parsedDestination;
            } else {
                newDestination = getDestination();
            }
        }

        if (currentDestination == null || !newDestination.getId().equals(currentDestination.getId())) {

            PermissionChecker.requireSend(newDestination);

            setDestination(newDestination.getId());

            reply.send(Icon.SETTING, "Destination set to %s `(%s)`%n",
                newDestination.getAsMention(), newDestination.getId()
            );
            getServer().log(Colors.GRAY, command.getMember().getUser().getId(),
                "Set destination of `%s` to %s (`%s`)", getName(), newDestination.getAsMention(),
                newDestination.getId());
        }

        this.start(reply);
        getServer().log(Colors.GREEN, command.getMember().getUser(),
            "Component `%s` started running", getName());
    }

    private void stopCommand(CommandEvent command, InteractionReply reply) {
        this.stop(reply);
        getServer().log(Colors.RED, command.getMember().getUser(),
            "Component `%s` stopped running", getName());
    }

    private void setDestination(CommandEvent command, InteractionReply reply) {
        TextChannel newDestination;
        if (command.hasOption("channel")) {
            newDestination = command.get("channel").getAsChannel().asTextChannel();
        } else {
            try {
                newDestination = command.getChannel().asTextChannel();
            } catch (IllegalStateException e) {
                throw new BotErrorException("Destination can only be a text channel");
            }
        }

        PermissionChecker.requirePermission(newDestination);

        setDestination(newDestination.getId());

        getServer().log(Colors.GRAY, command.getMember().getUser(), "Set `%s` destination channel to %s " +
            "(`%s`)", getName(), newDestination.getAsMention(), newDestination.getId());
        reply.send(Icon.SETTING, "Destination set to %s `(%s)`%n",
            newDestination.getAsMention(), newDestination.getId());
    }

    private void blacklistCommand(CommandEvent command, InteractionReply reply) {
        String action = command.get("action").getAsString();
        if (!command.hasOption("user")
            && ("add".equals(action) || "remove".equals(action))) {
            throw new BotErrorException("Please specify the user");
        }

        if ("show".equals(action)) {
            if (blacklist.isEmpty()) {
                throw new BotWarningException("No users are added to the blacklist");
            }

            long[] blacklistIds = new long[blacklist.size()];
            int i = 0;
            for (String s : blacklist) {
                blacklistIds[i] = Long.parseLong(s);
                ++i;
            }
            getGuild()
                .retrieveMembersByIds(blacklistIds)
                .onSuccess(list -> {
                    String[] strings = fillAbsent(blacklist, list, ISnowflake::getId,
                        IMentionable::getAsMention).toArray(String[]::new);
                    reply.send(new EmbedBuilder()
                        .setColor(Colors.TRANSPARENT)
                        .setTitle(getTitle() + " blacklist")
                        .setDescription(String.join("\n", strings))
                        .build());
                });
            return;
        }

        if ("clear".equals(action)) {
            if (blacklist.isEmpty()) {
                throw new BotWarningException("The blacklist is already empty");
            }

            blacklist.clear();
            getDatabaseManager().removeSetting("blacklist")
                .thenRun(() ->
                    reply.ok("Blacklist cleared")
                );
            return;
        }

        boolean add = "add".equals(action);
        String msg;

        if (add) {
            msg = "added to";
        } else {
            msg = "removed from";
        }

        User user = command.get("user").getAsUser();
        blacklist(user, add, msg, reply);
        if (add) {
            getServer().log(Colors.RED, command.getMember().getUser(),
                "Added %s (`%s`) to blacklist of `%s`", user.getAsMention(), user.getId(),
                getName());
        } else {
            getServer().log(Colors.GREEN, command.getMember().getUser(),
                "Removed %s (`%s`) from blacklist of `%s`", user.getAsMention(), user.getId(),
                getName());
        }
    }

    private void receive(Message message, MenuReply reply) {
        if (getDestination() == null) {
            throw new BotErrorException("Something went wrong");
        }
        if (getBlacklist().contains(message.getAuthor().getId())) {
            throw new BotWarningException("You are not allowed to send messages at the moment");
        }
        handleDirect(message, reply);
    }

    private void blacklist(@NotNull User user, boolean add, String msg, Reply reply) {
        String userId = user.getId();

        if (add) {
            if (blacklist.contains(userId)) {
                throw new BotWarningException("%s is already on the blacklist", user.getAsMention());
            }

            blacklist.add(userId);
            getDatabaseManager().addSetting("blacklist", userId);
        } else {
            if (!blacklist.contains(userId)) {
                throw new BotWarningException("%s is not on the blacklist", user.getAsMention());
            }

            blacklist.remove(userId);
            getDatabaseManager().removeSetting("blacklist", userId);
        }

        reply.ok("%s %s the blacklist", user.getAsMention(), msg);
    }

    @NotNull
    private <T, V> List<T> fillAbsent(Collection<T> expected, @NotNull Collection<V> actual,
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

    public Set<String> getBlacklist() {
        return blacklist;
    }

    protected abstract void handleDirect(Message message, MenuReply reply);

    public void stop(Reply reply) {
        getServer().getDirectMessageHandler().removeListener(this);
        this.running = false;
        if (autoRun) {
            getDatabaseManager().setSetting("running", "false");
        }
    }

    public void start(Reply reply) {
        this.running = true;
        getServer().getDirectMessageHandler().addListener(this, alt, this::receive);
        if (autoRun) {
            getDatabaseManager().setSetting("running", "true");
        }
    }

    public final void setDestination(String destination) {
        this.destination = destination;
        getDatabaseManager().setSetting("destination", destination);
    }

    @Nullable
    public final TextChannel getDestination() {
        if (destination == null) {
            return null;
        }
        return getGuild().getTextChannelById(destination);
    }

    @Override
    public boolean isRunning() {
        return running && isEnabled();
    }

    @Override
    public boolean isAutoRunnable() {
        return autoRun;
    }

}
