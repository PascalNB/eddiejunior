package com.thefatrat.application.components;

import com.thefatrat.application.Bot;
import com.thefatrat.application.entities.Command;
import com.thefatrat.application.entities.Reply;
import com.thefatrat.application.exceptions.BotErrorException;
import com.thefatrat.application.exceptions.BotWarningException;
import com.thefatrat.application.sources.Server;
import com.thefatrat.application.util.Colors;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public abstract class DirectComponent extends Component {

    private final boolean autoRun;
    private final Set<String> blacklist = new HashSet<>();
    private boolean running = false;
    private String destination;

    private final BiConsumer<Message, Reply> receiver = (message, reply) -> {
        if (getDestination() == null) {
            throw new BotErrorException("Something went wrong");
        }
        if (getBlacklist().contains(message.getAuthor().getId())) {
            throw new BotWarningException("You are not allowed to send messages at the moment");
        }
        handleDirect(message, reply);
    };

    public DirectComponent(Server server, String name, boolean autoRun) {
        super(server, name, false);
        this.autoRun = autoRun;
        new Thread(() -> {
            destination = getDatabaseManager().getSetting("destination");
            if (autoRun && Boolean.parseBoolean(getDatabaseManager().getSettingOr("running", "false"))) {
                start(Reply.empty());
            }
            blacklist.addAll(getDatabaseManager().getSettings("blacklist"));
        }).start();

        addCommands(new Command(getName(), "component command")
            .setAction((command, reply) -> getSubCommandHandler().handle(command.toSub(), reply))
            .addSubcommand(new Command("start", "starts the component")
                .addOption(new OptionData(OptionType.CHANNEL, "channel", "channel destination")
                    .setChannelTypes(ChannelType.TEXT)
                )
                .setAction((command, reply) -> {
                    OptionMapping option = command.getArgs().get("channel");
                    TextChannel parsedDestination = option == null ? null : option.getAsChannel().asTextChannel();
                    TextChannel newDestination;

                    if (getDestination() == null && parsedDestination == null) {
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

                    if (getDestination() == null || !newDestination.getId().equals(getDestination().getId())) {
                        setDestination(newDestination.getId());

                        reply.sendEmbedFormat(Colors.GRAY, ":gear: Destination set to %s `(%s)`%n",
                            getDestination().getAsMention(), getDestination().getId()
                        );
                    }

                    start(reply);
                })
            )
            .addSubcommand(new Command("stop", "stops the component")
                .setAction((command, reply) -> this.stop(reply))
            )
            .addSubcommand(new Command("destination", "sets the destination channel")
                .addOption(new OptionData(OptionType.CHANNEL, "channel", "destination channel", false)
                    .setChannelTypes(ChannelType.TEXT)
                )
                .setAction((command, reply) -> {
                    TextChannel newDestination;
                    if (command.getArgs().containsKey("channel")) {
                        newDestination = command.getArgs().get("channel").getAsChannel().asTextChannel();
                    } else {
                        try {
                            newDestination = command.getChannel().asTextChannel();
                        } catch (IllegalStateException e) {
                            throw new BotErrorException("Destination can only be a text channel");
                        }
                    }

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
                            .retrieveMembersByIds(
                                blacklist.stream()
                                    .map(Long::parseLong)
                                    .collect(Collectors.toList())
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
                        getDatabaseManager().removeSetting("blacklist")
                            .thenRun(() ->
                                reply.sendEmbedFormat(Colors.GREEN, ":white_check_mark: Blacklist cleared")
                            )
                            .join();
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
            getDatabaseManager().addSetting("blacklist", userId).join();
        } else {
            if (!blacklist.contains(userId)) {
                throw new BotWarningException(String.format("%s is not on the blacklist",
                    member.getAsMention()));
            }

            blacklist.remove(userId);
            getDatabaseManager().removeSetting("blacklist", userId).join();
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

    protected void stop(Reply reply) {
        getServer().getDirectHandler().removeListener(getTitle());
        this.running = false;
        if (autoRun) {
            getDatabaseManager().setSetting("running", "false").join();
        }
    }

    protected void start(Reply reply) {
        this.running = true;
        getServer().getDirectHandler().addListener(getTitle(), receiver);
        if (autoRun) {
            getDatabaseManager().setSetting("running", "true").join();
        }
    }

    public void setDestination(String destination) {
        this.destination = destination;
        getDatabaseManager().setSetting("destination", destination).join();
    }

    public TextChannel getDestination() {
        if (destination == null) {
            return null;
        }
        return Bot.getInstance().getJDA().getChannelById(TextChannel.class, destination);
    }

    public boolean isRunning() {
        return running && isEnabled();
    }

}
