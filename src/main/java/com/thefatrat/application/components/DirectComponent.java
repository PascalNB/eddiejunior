package com.thefatrat.application.components;

import com.thefatrat.application.Bot;
import com.thefatrat.application.exceptions.BotErrorException;
import com.thefatrat.application.exceptions.BotWarningException;
import com.thefatrat.application.sources.Server;
import com.thefatrat.application.util.Colors;
import com.thefatrat.application.util.Command;
import com.thefatrat.application.util.CommandEvent;
import com.thefatrat.application.util.Reply;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public abstract class DirectComponent extends Component {

    private final Set<String> blacklist = new HashSet<>();
    private boolean running = false;
    private boolean paused = false;
    private String destination;

    private final BiConsumer<Message, Reply> receiver = (message, reply) -> {
        if (getDestination() == null) {
            return;
        }
        if (getBlacklist().contains(message.getAuthor().getId())) {
            throw new BotWarningException("You are not allowed to send messages at the moment");
        }
        handleDirect(message, reply);
    };

    public DirectComponent(Server server, String name) {
        super(server, name, false);

        new Thread(() -> {
            destination = getDatabaseManager().getSetting("destination");
            blacklist.addAll(getDatabaseManager().getSettings("blacklist"));
        }).start();

        addCommands(new Command(getName(), "component command")
            .setAction((command, reply) -> getSubHandler().handle(command.toSub(), reply))
            .addSubcommand(new Command("start", "starts the component")
                .addOption(new OptionData(OptionType.CHANNEL, "channel", "channel destination")
                    .setChannelTypes(ChannelType.TEXT)
                )
                .setAction((command, reply) -> {
                    MessageChannel parsedDestination = Optional.ofNullable(command.getArgs().get("channel"))
                        .map(option -> command.getGuild().getChannelById(MessageChannel.class,
                            option.getAsChannel().getId())
                        )
                        .orElse(command.getChannel());
                    MessageChannel newDestination;

                    if (parsedDestination != null) {
                        newDestination = parsedDestination;
                    } else {
                        newDestination = getDestination() == null
                            ? command.getChannel()
                            : getDestination();
                    }

                    if (getDestination() == null ||
                        !newDestination.getId().equals(getDestination().getId())) {
                        setDestination(newDestination.getId());

                        reply.sendEmbedFormat(Colors.GRAY, ":gear: Destination set to %s `(%s)`%n",
                            getDestination().getAsMention(), getDestination().getId()
                        );
                    }

                    List<DirectComponent> pausedComponents = new ArrayList<>();
                    for (DirectComponent component : getServer().getDirectComponents()) {
                        if (component != this && component.isRunning() &&
                            !component.isPaused()) {
                            component.setPaused(true);
                            pausedComponents.add(component);
                        }
                    }
                    if (pausedComponents.size() > 0) {
                        StringBuilder builder = new StringBuilder()
                            .append(":pause_button: The following components were paused:");
                        for (Component component : pausedComponents) {
                            builder.append(" ").append(component.getTitle()).append(",");
                        }
                        builder.deleteCharAt(builder.length() - 1);
                        reply.sendEmbedFormat(Colors.GRAY, builder.toString());
                    }

                    start(command, reply);
                })
            )
            .addSubcommand(new Command("stop", "stops the component")
                .setAction(this::stop)
            )
            .addSubcommand(new Command("destination", "sets the destination channel")
                .addOption(new OptionData(OptionType.CHANNEL, "channel", "destination channel", false)
                    .setChannelTypes(ChannelType.TEXT)
                )
                .setAction((command, reply) -> {
                    MessageChannel newDestination = Optional.ofNullable(
                            command.getArgs().get("channel"))
                        .map(option -> command.getGuild()
                            .getChannelById(MessageChannel.class, option.getAsChannel().getId())
                        )
                        .orElse(command.getChannel());

                    setDestination(newDestination.getId());

                    reply.sendEmbedFormat(Colors.GRAY, ":gear: Destination set to %s `(%s)`%n",
                        getDestination().getAsMention(), getDestination().getId());
                })
            )
            .addSubcommand(new Command("blacklist", "manages the blacklist")
                .addOption(new OptionData(OptionType.STRING, "action", "action", true)
                    .addChoice("add", "add")
                    .addChoice("remove", "remove")
                    .addChoice("show", "show")
                    .addChoice("clear", "clear")
                )
                .addOption(new OptionData(OptionType.USER, "user", "user", false))
                .setAction((command, reply) -> {
                    String action = command.getArgs().get("action").getAsString();
                    if (!command.getArgs().containsKey("user")
                        && ("add".equals(action) || "remove".equals(action))) {
                        throw new BotErrorException("Please specify the user");
                    }

                    if ("show".equals(action)) {
                        if (blacklist.isEmpty()) {
                            throw new BotWarningException(
                                "No users are added to the blacklist");
                        }

                        command.getGuild()
                            .retrieveMembersByIds(blacklist.stream()
                                .map(Long::parseLong).collect(Collectors.toList())
                            )
                            .onSuccess(list -> {
                                String[] strings = fillAbsent(blacklist, list,
                                    ISnowflake::getId, IMentionable::getAsMention)
                                    .toArray(String[]::new);
                                reply.sendEmbed(new EmbedBuilder()
                                    .setColor(Colors.WHITE)
                                    .addField("Blacklist",
                                        String.join("\n", strings), false)
                                    .build());
                            });
                        return;
                    }

                    if ("clear".equals(action)) {
                        if (blacklist.isEmpty()) {
                            throw new BotWarningException("The blacklist is already empty");
                        }

                        blacklist.clear();
                        getDatabaseManager().removeSetting("blacklist");
                        reply.sendEmbedFormat(Colors.GREEN, ":white_check_mark: Blacklist cleared");
                        return;
                    }

                    boolean add = "add".equals(action);
                    String msg;

                    if (add) {
                        msg = "added to";
                    } else {
                        msg = "removed from";
                    }

                    Member member = command.getArgs().get("user").getAsMember();

                    if (member != null) {
                        blacklist(member, add, msg, reply);
                        return;
                    }
                    throw new BotErrorException("The given member was not found");
                })
            )
        );
    }

    private void blacklist(Member member, boolean add, String msg, Reply reply) {
        User user = member.getUser();
        String userId = member.getId();

        if (add) {
            if (blacklist.contains(userId)) {
                throw new BotWarningException(String.format("%s is already on the blacklist",
                    member.getAsMention()));
            }

            blacklist.add(userId);
            getDatabaseManager().addSetting("blacklist", userId);
        } else {
            if (!blacklist.contains(userId)) {
                throw new BotWarningException(String.format("%s is not on the blacklist",
                    member.getAsMention()));
            }

            blacklist.remove(userId);
            getDatabaseManager().removeSetting("blacklist", userId);
        }

        reply.sendEmbedFormat(Colors.GREEN, ":white_check_mark: %s %s the blacklist",
            user.getAsMention(), msg);
    }

    @Override
    public void disable() {
        super.disable();
    }

    public Set<String> getBlacklist() {
        return blacklist;
    }

    protected abstract void handleDirect(Message message, Reply reply);

    protected void stop(CommandEvent command, Reply reply) {
        getServer().getDirectHandler().removeListener(receiver);
        this.running = false;
        this.paused = false;
    }

    protected void start(CommandEvent command, Reply reply) {
        this.paused = false;
        this.running = true;
        getServer().getDirectHandler().addListener(receiver);
    }

    public void setDestination(String destination) {
        this.destination = destination;
        getDatabaseManager().setSetting("destination", destination);
    }

    public MessageChannel getDestination() {
        if (destination == null) {
            return null;
        }
        return Bot.getInstance().getJDA().getChannelById(MessageChannel.class, destination);
    }

    public boolean isRunning() {
        return running && isEnabled();
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
        if (paused) {
            getServer().getDirectHandler().removeListener(receiver);
        } else {
            getServer().getDirectHandler().addListener(receiver);
        }
    }

    public boolean isPaused() {
        return paused;
    }

}
