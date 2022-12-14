package com.thefatrat.application.components;

import com.thefatrat.application.entities.Command;
import com.thefatrat.application.entities.Reply;
import com.thefatrat.application.exceptions.BotErrorException;
import com.thefatrat.application.exceptions.BotWarningException;
import com.thefatrat.application.sources.Server;
import com.thefatrat.application.util.Colors;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.ThreadMember;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.internal.utils.PermissionUtil;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class ModMail extends DirectComponent {

    public static final String NAME = "Modmail";

    private final Map<String, Long> timeouts = new ConcurrentHashMap<>();
    private final Map<String, Integer> userCount = new ConcurrentHashMap<>();
    private int tickets = 0;
    private long timeout;
    private long threadId;
    private int maxTickets;
    private int maxTicketsPerUser;
    private boolean privateThreads;

    public ModMail(Server server) {
        super(server, NAME, true);

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
                    getDatabaseManager().setSetting("timeout", String.valueOf(timeout))
                        .thenRun(() -> reply.send(Colors.GREEN,
                            ":white_check_mark: Timout set to %d seconds", timeout));
                }),

            new Command("archive", "archives the current ticket thread")
                .setAction((command, reply) -> {
                    if (!command.getChannel().getType().equals(ChannelType.GUILD_PRIVATE_THREAD)
                        && !command.getChannel().getType().equals(ChannelType.GUILD_PUBLIC_THREAD)) {
                        throw new BotErrorException("Cannot archive this channel");
                    }
                    reply.send(callback ->
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
                    getDatabaseManager().setSetting("maxticketsperuser", String.valueOf(max))
                        .thenRun(() -> reply.send(Colors.GREEN,
                            ":white_check_mark: Maximum set to %d tickets", max));
                }),

            new Command("maxtickets", "sets the maximum number of overall tickets")
                .addOption(new OptionData(OptionType.INTEGER, "max", "number of tickets", true)
                    .setRequiredRange(0, Integer.MAX_VALUE)
                )
                .setAction((command, reply) -> {
                    int max = command.getArgs().get("max").getAsInt();
                    this.maxTickets = max;
                    getDatabaseManager().setSetting("maxtickets", String.valueOf(max))
                        .thenRun(() -> reply.send(Colors.GREEN,
                            ":white_check_mark: Maximum set to %d tickets", max));
                }),

            new Command("privatethreads", "determines if the created threads are private or public")
                .addOption(new OptionData(OptionType.BOOLEAN, "value", "true or false", true))
                .setAction((command, reply) -> {
                    boolean value = command.getArgs().get("value").getAsBoolean();
                    this.privateThreads = value;
                    getDatabaseManager().setSetting("privatethreads", String.valueOf(value))
                        .thenRun(() -> reply.send(Colors.GREEN,
                            ":white_check_mark: Thread creation set to %s", value ? "private" : "public"));
                }),

            new Command("recheck", "rechecks the amount of open tickets")
                .setAction((command, reply) -> {
                    if (getDestination() == null) {
                        throw new BotErrorException("Destination channel has not been set");
                    }
                    tickets = 0;
                    for (ThreadChannel threadChannel : getDestination().getThreadChannels()) {
                        if (!threadChannel.isArchived() && threadChannel.getName().matches("^t\\d+-.+$")) {
                            tickets++;
                        }
                    }
                    reply.send(Colors.GREEN,
                        ":white_check_mark: Recheck completed, found **%d** open tickets", tickets);
                })
        );

        getServer().getArchiveHandler().addListener(getName(), event -> {
            String name = event.getThread().getName();

            if (!name.matches("^t\\d+-.+$")) {
                return;
            }

            --tickets;

            event.getThread().retrieveThreadMembers().queue(members -> {
                for (ThreadMember member : members) {
                    String id = member.getId();
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
            isEnabled(), isRunning(), dest, timeout, tickets, maxTickets, maxTicketsPerUser,
            privateThreads);
    }

    @Override
    protected synchronized void handleDirect(Message message, Reply reply) {
        String content = message.getContentRaw();
        if (content.length() < 20 || content.length() > 1024) {
            throw new BotWarningException("Messages have to be between 20 and 1024 characters");
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

        if (!PermissionUtil.checkPermission(getDestination().getPermissionContainer(),
            getServer().getGuild().getSelfMember(), Permission.MESSAGE_EMBED_LINKS, Permission.MANAGE_THREADS,
            (privateThreads ? Permission.CREATE_PRIVATE_THREADS : Permission.CREATE_PUBLIC_THREADS))) {
            throw new BotErrorException("If you see this error, the server admins messed up");
        }

        timeouts.put(author.getId(), System.currentTimeMillis());
        userCount.put(author.getId(), userCount.getOrDefault(author.getId(), 0) + 1);
        ++threadId;
        CompletableFuture<Void> future = getDatabaseManager().setSetting("threadid", String.valueOf(threadId));
        String topic = String.format("t%d-%s", threadId, author.getAsTag());
        topic = topic.substring(0, Math.min(topic.length(), 25));

        getDestination()
            .createThreadChannel(topic, privateThreads)
            .queue(thread -> {
                List<String> list = new ArrayList<>();

                for (Message.Attachment attachment : message.getAttachments()) {
                    list.add(attachment.getUrl());
                }

                String urls = String.join("\n", list.toArray(new String[0]));

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
            });

        ++tickets;
        future.thenRun(() -> reply.send(Colors.GREEN,
            ":white_check_mark: Message successfully submitted"));
    }

    protected void stop(Reply reply) {
        super.stop(reply);
        reply.send(Colors.GREEN,
            ":stop_sign: Mod mail service stopped",
            getDestination().getId()
        );
    }

    protected void start(Reply reply) {
        super.start(reply);
        reply.send(Colors.GREEN,
            ":white_check_mark: Mod mail service started",
            getDestination().getId()
        );
    }

}
