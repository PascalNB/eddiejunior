package com.thefatrat.application.components;

import com.thefatrat.application.Bot;
import com.thefatrat.application.events.CommandEvent;
import com.thefatrat.application.exceptions.BotErrorException;
import com.thefatrat.application.exceptions.BotWarningException;
import com.thefatrat.application.sources.Server;
import com.thefatrat.application.util.Colors;
import com.thefatrat.application.util.Command;
import com.thefatrat.application.util.Reply;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Channel;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.utils.data.DataArray;
import net.dv8tion.jda.internal.requests.RestActionImpl;
import net.dv8tion.jda.internal.requests.Route;

import java.time.Instant;
import java.util.*;

public class ModMail extends DirectComponent {

    public static final String NAME = "Modmail";

    private final Map<String, Long> timeouts = new HashMap<>();
    private final Map<String, Integer> userCount = new HashMap<>();
    private int tickets = 0;
    private long timeout;
    private long threadId;
    private int maxTickets;
    private int maxTicketsPerUser;
    private boolean privateThreads;

    public ModMail(Server server) {
        super(server, NAME);

        timeout = Long.parseLong(
            getDatabaseManager().getSettingOr("timeout", 0)
        );
        threadId = Long.parseLong(
            getDatabaseManager().getSettingOr("threadid", 0)
        );
        maxTickets = Integer.parseInt(
            getDatabaseManager().getSettingOr("maxtickets", 0)
        );
        maxTicketsPerUser = Integer.parseInt(
            getDatabaseManager().getSettingOr("maxticketsperuser", 0)
        );
        privateThreads = Boolean.parseBoolean(
            getDatabaseManager().getSettingOr("privatethreads", false)
        );
        if (getDestination() != null) {
            getDestination().getThreadChannels().forEach(threadChannel -> {
                if (!threadChannel.isArchived() && threadChannel.getName().matches("^t\\d+-.+$")) {
                    tickets++;
                }
            });
        }

        addSubcommands(
            new Command("timeout", "sets the timeout")
                .addOption(new OptionData(OptionType.INTEGER, "timeout", "timeout in ms", true)
                    .setRequiredRange(0, Integer.MAX_VALUE)
                )
                .setAction((command, reply) -> {
                    long timeout = command.getArgs().get("timeout").getAsInt();
                    this.timeout = timeout;
                    getDatabaseManager().setSetting("timeout", String.valueOf(timeout));
                    reply.sendEmbedFormat(Colors.GREEN,
                        ":white_check_mark: Timout set to %d seconds", timeout);
                }),

            new Command("archive", "archives the current ticket thread")
                .setAction((command, reply) -> {
                    if (!command.getChannel().getType().equals(ChannelType.GUILD_PRIVATE_THREAD)
                        && !command.getChannel().getType().equals(ChannelType.GUILD_PUBLIC_THREAD)) {
                        throw new BotErrorException("Cannot archive this channel");
                    }
                    reply.sendEmbedFormat(callback ->
                        command.getChannel().asThreadChannel().getManager().setLocked(true).queue(success ->
                            command.getChannel().asThreadChannel().getManager().setArchived(true).queue()
                        ), Colors.GREEN, ":white_check_mark: Current thread archived");
                }),

            new Command("maxperuser", "sets the maximum number of active tickets per user")
                .addOption(new OptionData(OptionType.INTEGER, "max", "number of tickets", true)
                    .setRequiredRange(0, Integer.MAX_VALUE)
                )
                .setAction((command, reply) -> {
                    int max = command.getArgs().get("max").getAsInt();
                    this.maxTicketsPerUser = max;
                    getDatabaseManager().setSetting("maxticketsperuser", String.valueOf(max));
                    reply.sendEmbedFormat(Colors.GREEN,
                        ":white_check_mark: Maximum set to %d tickets", max);
                }),

            new Command("maxtickets", "sets the maximum number of overall tickets")
                .addOption(new OptionData(OptionType.INTEGER, "max", "number of tickets", true)
                    .setRequiredRange(0, Integer.MAX_VALUE)
                )
                .setAction((command, reply) -> {
                    int max = command.getArgs().get("max").getAsInt();
                    this.maxTickets = max;
                    getDatabaseManager().setSetting("maxtickets", String.valueOf(max));
                    reply.sendEmbedFormat(Colors.GREEN,
                        ":white_check_mark: Maximum set to %d tickets", max);
                }),

            new Command("privatethreads", "determines if the created threads are private or public")
                .addOption(new OptionData(OptionType.BOOLEAN, "value", "true or false", true))
                .setAction((command, reply) -> {
                    if (command.getGuild().getBoostCount() < 2) {
                        throw new BotErrorException("The server needs 2 boosts to use private threads");
                    }
                    boolean value = command.getArgs().get("value").getAsBoolean();
                    this.privateThreads = value;
                    getDatabaseManager().setSetting("privatethreads", String.valueOf(value));
                    reply.sendEmbedFormat(Colors.GREEN,
                        ":white_check_mark: Thread creation set to %s", value ? "private" : "public");
                }),

            new Command("recheck", "rechecks the amount of open tickets")
                .setAction((command, reply) -> {
                    if (getDestination() == null) {
                        throw new BotErrorException("Destination channel has not been set");
                    }
                    tickets = 0;
                    getDestination().getThreadChannels().forEach(threadChannel -> {
                        if (!threadChannel.isArchived() && threadChannel.getName().matches("^t\\d+-.+$")) {
                            tickets++;
                        }
                    });
                    reply.sendEmbedFormat(Colors.GREEN,
                        ":white_check_mark: Recheck completed, found **%d** open tickets", tickets);
                })
        );

        getServer().getArchiveHandler().addListener(getName(), event -> {
            String name = event.getThread().getName();

            if (!name.matches("^t\\d+-.+$")) {
                return;
            }

            --tickets;

            Route.CompiledRoute route = Route.Channels.LIST_THREAD_MEMBERS.compile(event.getThread().getId());

            RestAction<List<String>> action = new RestActionImpl<>(Bot.getInstance().getJDA(), route,
                (response, request) -> {
                    List<String> users = new LinkedList<>();
                    DataArray memberArr = response.getArray();

                    for (int i = 0; i < memberArr.length(); ++i) {
                        users.add(memberArr.getObject(i).get("user_id").toString());
                    }

                    return users;
                });

            action.queue(users -> {
                for (String id : users) {
                    if (!userCount.containsKey(id)) {
                        continue;
                    }
                    int count = userCount.get(id) - 1;
                    if (count <= 0) {
                        userCount.remove(id);
                        timeouts.remove(id);
                    } else {
                        userCount.put(id, count);
                    }
                }
            });
        });
    }

    @Override
    public String getStatus() {
        String dest = Optional.ofNullable(getDestination())
            .map(Channel::getAsMention)
            .orElse(null);
        return String.format("""
                Enabled: %b
                Running: %b
                Destination: %s
                Timeout: %d sec
                Open tickets: %d
                Max tickets: %d
                Max tickets per user: %d
                Private tickets: %b
                """,
            isEnabled(), isRunning() && !isPaused(), dest, timeout, tickets, maxTickets, maxTicketsPerUser,
            privateThreads);
    }

    @Override
    protected void handleDirect(Message message, Reply reply) {
        String content = message.getContentRaw();
        if (content.length() < 20) {
            throw new BotWarningException("Messages have to be at least 20 characters");
        }

        if (maxTickets != 0 && tickets == maxTickets) {
            throw new BotWarningException("The server does not accept tickets at this moment");
        }

        User author = message.getAuthor();
        if (maxTicketsPerUser != 0 && userCount.containsKey(author.getId())
            && userCount.get(author.getId()) >= maxTicketsPerUser) {
            throw new BotWarningException(
                String.format("You can only have %d open tickets at the same time", maxTicketsPerUser));
        }

        if (System.currentTimeMillis() - timeouts.getOrDefault(author.getId(), 0L) < timeout * 1000L) {
            throw new BotWarningException(
                String.format("You can only send a message every %d seconds", timeout));
        }

        timeouts.put(author.getId(), System.currentTimeMillis());
        userCount.put(author.getId(), userCount.getOrDefault(author.getId(), 0) + 1);
        ++threadId;
        getDatabaseManager().setSetting("threadid", String.valueOf(threadId));
        String topic = String.format("t%d-%s", threadId, author.getAsTag());
        topic = topic.substring(0, Math.min(topic.length(), 25));
        getDestination()
            .createThreadChannel(topic, privateThreads)
            .queue(thread -> {
                String urls = String.join("\n",
                    message.getAttachments().stream().map(Message.Attachment::getUrl).toArray(String[]::new));
                EmbedBuilder embed = new EmbedBuilder()
                    .setColor(Colors.LIGHT)
                    .setAuthor(author.getAsTag(), null, author.getEffectiveAvatarUrl())
                    .addField("User", String.format("%s `%s`", author.getAsMention(), author.getId()), false)
                    .addField("Message", String.format("```%s```", content), false)
                    .setFooter(getName())
                    .setTimestamp(Instant.now());
                if (urls.length() > 1) {
                    embed.addField("Attachments", String.format("```%s```", urls), false);
                }
                thread.sendMessageEmbeds(embed.build()).queue();
                thread.addThreadMember(author).queue();
                thread.getManager().setSlowmode(10);
                if (privateThreads) {
                    thread.getManager().setInvitable(false);
                }
            });
        ++tickets;
        reply.sendEmbedFormat(Colors.GREEN,
            ":white_check_mark: Message successfully submitted");
    }

    @Override
    protected void stop(CommandEvent command, Reply reply) {
        super.stop(command, reply);
        reply.sendEmbedFormat(Colors.GREEN,
            ":stop_sign: Mod mail service stopped",
            getDestination().getId()
        );
    }

    @Override
    protected void start(CommandEvent command, Reply reply) {
        super.start(command, reply);
        reply.sendEmbedFormat(Colors.GREEN,
            ":white_check_mark: Mod mail service started",
            getDestination().getId()
        );
    }

}
