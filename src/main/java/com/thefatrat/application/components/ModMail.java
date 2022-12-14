package com.thefatrat.application.components;

import com.thefatrat.application.entities.Command;
import com.thefatrat.application.exceptions.BotErrorException;
import com.thefatrat.application.exceptions.BotWarningException;
import com.thefatrat.application.reply.Reply;
import com.thefatrat.application.sources.Server;
import com.thefatrat.application.util.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.IMentionable;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.ThreadMember;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.internal.utils.PermissionUtil;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ModMail extends DirectComponent {

    public static final String NAME = "Modmail";

    private static final Pattern MENTION_PATTERN = Pattern.compile("^.*(\\d+).*$");

    private final Map<String, Long> timeouts = new ConcurrentHashMap<>();
    private final Map<String, Integer> userCount = new ConcurrentHashMap<>();
    private int tickets = 0;
    private long timeout;
    private long threadId;
    private int maxTickets;
    private int maxTicketsPerUser;
    private boolean privateThreads;
    private IMentionable mention;

    public ModMail(Server server) {
        super(server, NAME, true);

        {
            Map<String, StringMapping> settings = getSettings("timeout", "threadid", "maxtickets", "maxticketsperuser",
                "privatethreads", "mention");

            timeout = settings.get("timeout").asOrDefault(0);
            threadId = settings.get("threadid").asOrDefault(0);
            maxTickets = settings.get("maxtickets").asOrDefault(0);
            maxTicketsPerUser = settings.get("maxticketsperuser").asOrDefault(0);
            privateThreads = settings.get("privatethreads").asOrDefault(false);
            mention = settings.get("mention").applyIfNotNull(string -> {
                Matcher matcher = MENTION_PATTERN.matcher(string);
                if (matcher.find()) {
                    long id = Long.parseLong(matcher.group(1));
                    return new IMentionable() {
                        @NotNull
                        @Override
                        public String getAsMention() {
                            return string;
                        }

                        @Override
                        public long getIdLong() {
                            return id;
                        }
                    };
                }
                return null;
            });

            if (getDestination() != null) {
                getDestination().getThreadChannels().forEach(threadChannel -> {
                    if (!threadChannel.isArchived() && threadChannel.getName().matches("^t\\d+-.+$")) {
                        tickets++;
                    }
                });
            }
        }

        addSubcommands(
            new Command("message", "creates a new message with modmail button")
                .addOptions(
                    new OptionData(OptionType.STRING, "message", "jump url of a message to use as content", true),
                    new OptionData(OptionType.CHANNEL, "channel", "channel to send the message in", false)
                        .setChannelTypes(ChannelType.TEXT)
                )
                .setAction((command, reply) -> {
                    String url = command.getArgs().get("message").getAsString();
                    Message message = URLUtil.messageFromURL(url, getServer().getGuild());

                    MessageCreateBuilder builder = new MessageCreateBuilder();

                    try (MessageCreateData data = MessageCreateData.fromMessage(message)) {
                        builder.applyData(data);
                        builder.setComponents();
                    } catch (IllegalArgumentException e) {
                        throw new BotErrorException("Couldn't use referenced message");
                    } catch (IllegalStateException e) {
                        throw new BotErrorException("Cannot reference an empty message");
                    }

                    Button button = Button.success("modmail-modal", "New ticket").withEmoji(Emoji.fromUnicode("???"));
                    builder.setComponents(ActionRow.of(button));

                    if (builder.isEmpty()) {
                        throw new BotErrorException("Cannot reference a message without content");
                    }

                    OptionMapping channelObject = command.getArgs().get("channel");

                    if (channelObject == null) {
                        reply.send(builder.build());
                        return;
                    }

                    TextChannel channel = channelObject.getAsChannel().asTextChannel();

                    PermissionChecker.requireSend(channel);

                    channel.sendMessage(builder.build()).queue();
                    reply.ok("Message sent in %s", channel.getAsMention());
                }),

            new Command("timeout", "sets the timeout")
                .addOption(new OptionData(OptionType.INTEGER, "timeout", "timeout in seconds", true)
                    .setRequiredRange(0, Integer.MAX_VALUE)
                )
                .setAction((command, reply) -> {
                    long timeout = command.getArgs().get("timeout").getAsInt();
                    this.timeout = timeout;
                    reply.ok("Timout set to %d seconds", timeout);
                    getDatabaseManager().setSetting("timeout", String.valueOf(timeout));
                }),

            new Command("archive", "archives the current ticket thread")
                .setAction((command, reply) -> {
                    if (!command.getChannel().getType().equals(ChannelType.GUILD_PRIVATE_THREAD)
                        && !command.getChannel().getType().equals(ChannelType.GUILD_PUBLIC_THREAD)) {
                        throw new BotErrorException("Cannot archive this channel");
                    }
                    reply.ok(callback ->
                        command.getChannel().asThreadChannel().getManager().setLocked(true).queue(success ->
                            command.getChannel().asThreadChannel().getManager().setArchived(true).queue()
                        ), "Current thread archived");
                }),

            new Command("maxperuser", "sets the maximum number of active tickets per user")
                .addOption(new OptionData(OptionType.INTEGER, "max", "number of tickets", true)
                    .setRequiredRange(0, Integer.MAX_VALUE)
                )
                .setAction((command, reply) -> {
                    int max = command.getArgs().get("max").getAsInt();
                    this.maxTicketsPerUser = max;
                    reply.ok("Maximum set to %d tickets", max);
                    getDatabaseManager().setSetting("maxticketsperuser", String.valueOf(max));
                }),

            new Command("maxtickets", "sets the maximum number of overall tickets")
                .addOption(new OptionData(OptionType.INTEGER, "max", "number of tickets", true)
                    .setRequiredRange(0, Integer.MAX_VALUE)
                )
                .setAction((command, reply) -> {
                    int max = command.getArgs().get("max").getAsInt();
                    this.maxTickets = max;
                    reply.ok("Maximum set to %d tickets", max);
                    getDatabaseManager().setSetting("maxtickets", String.valueOf(max));
                }),

            new Command("privatethreads", "determines if the created threads are private or public")
                .addOption(new OptionData(OptionType.BOOLEAN, "value", "true or false", true))
                .setAction((command, reply) -> {
                    boolean value = command.getArgs().get("value").getAsBoolean();
                    this.privateThreads = value;
                    reply.ok("Thread creation set to %s", value ? "private" : "public");
                    getDatabaseManager().setSetting("privatethreads", String.valueOf(value));
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
                    reply.ok("Recheck completed, found **%d** open tickets", tickets);
                }),

            new Command("mention", "set which role the bot should mention upon a new ticket")
                .addOption(new OptionData(OptionType.MENTIONABLE, "mention", "mention", false))
                .setAction(((command, reply) -> {
                    if (!command.getArgs().containsKey("mention")) {
                        if (mention == null) {
                            throw new BotErrorException("Please provide a mentionable target");
                        }
                        reply.ok("Removed mention %s", mention.getAsMention());
                        mention = null;
                        getDatabaseManager().removeSetting("mention");
                        return;
                    }

                    IMentionable newMention = command.getArgs().get("mention").getAsMentionable();
                    if (Message.MentionType.EVERYONE.getPattern().matcher(newMention.getAsMention()).matches()) {
                        throw new BotErrorException("Cannot mention %s", newMention.getAsMention());
                    }
                    mention = newMention;
                    reply.ok("Set mention to %s", mention.getAsMention());
                    getDatabaseManager().setSetting("mention", mention.getAsMention());
                }))
        );

        getServer().getArchiveHandler().addListener((event, reply) -> {
            String name = event.getThread().getName();

            if (!name.matches("^t\\d+-.+$")) {
                return;
            }

            --tickets;

            synchronized (userCount) {
                synchronized (timeouts) {
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
                }
            }
        });

        getServer().getButtonHandler().addListener((event, reply) -> {
            String buttonId = event.getButtonId();
            if (!buttonId.startsWith("modmail-")) {
                return;
            }

            String[] split = buttonId.split("-");
            String action = split[1];

            if ("archive".equals(action)) {
                String threadId = split[2];
                ThreadChannel thread = getServer().getGuild().getThreadChannelById(threadId);

                if (thread == null) {
                    throw new BotErrorException("Thread not found");
                }

                PermissionChecker.requirePermission(thread.getParentChannel(), Permission.MANAGE_THREADS);

                reply.ok(callback ->
                        thread.getManager().setLocked(true).queue(success ->
                            thread.getManager().setArchived(true).queue()
                        ),
                    "Current thread archived");
            }
        });
        getServer().getButtonHandler().addListener((event, reply) -> {
            String buttonId = event.getButtonId();
            if (!buttonId.startsWith("modmail-")) {
                return;
            }

            String[] split = buttonId.split("-");
            String action = split[1];

            if ("modal".equals(action)) {
                if (!isRunning()) {
                    throw new BotWarningException("The server does not accept tickets at the moment");
                }

                TextInput subject = TextInput.create("subject", "Subject", TextInputStyle.SHORT)
                    .setRequired(true)
                    .setPlaceholder("Subject of this ticket")
                    .setRequiredRange(6, 90)
                    .setValue(event.getUser().getUser().getAsTag())
                    .build();

                TextInput message = TextInput.create("message", "Message", TextInputStyle.PARAGRAPH)
                    .setRequired(true)
                    .setPlaceholder("Type your message here")
                    .setRequiredRange(20, 4000)
                    .build();

                Modal modal = Modal.create("modmail", "Modmail")
                    .addActionRow(subject)
                    .addActionRow(message)
                    .build();

                reply.sendModal(modal);
            }
        });
        getServer().getModalHandler().addListener("modmail", (event, reply) -> {
            String subject = event.getValues().get("subject").getAsString();
            String message = event.getValues().get("message").getAsString();
            reply.hide();
            synchronized (userCount) {
                synchronized (timeouts) {
                    createTicket(event.getMember().getUser(), subject, message, List.of(), reply);
                }
            }
        });
    }

    @Override
    public String getStatus() {
        String dest = Optional.ofNullable(getDestination())
            .map(Channel::getAsMention)
            .orElse(null);
        String ment = Optional.ofNullable(mention)
            .map(IMentionable::getAsMention)
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
                Mention: %s
                """,
            isEnabled(), isRunning(), dest, timeout, tickets, maxTickets, maxTicketsPerUser,
            privateThreads, ment);
    }

    private synchronized void createTicket(User author, String subject, String message,
        List<Message.Attachment> attachments, Reply reply) {
        if (!isRunning()) {
            throw new BotWarningException("The server does not accept tickets at the moment");
        }
        if (message.length() < 20) {
            throw new BotWarningException("Messages have to be between at least 20 characters");
        }
        if (message.length() > 4090) {
            throw new BotWarningException("Message cannot be longer than 4090 characters");
        }
        if (!message.contains(" ")) {
            throw new BotWarningException("Spam is not allowed");
        }

        if (maxTickets != 0 && tickets == maxTickets) {
            throw new BotWarningException("The server does not accept tickets at this moment");
        }

        if (maxTicketsPerUser != 0 && userCount.containsKey(author.getId())
            && userCount.get(author.getId()) >= maxTicketsPerUser) {
            throw new BotWarningException("You can only have %d open tickets at the same time", maxTicketsPerUser);
        }

        if (System.currentTimeMillis() - timeouts.getOrDefault(author.getId(), 0L) < timeout * 1000L) {
            throw new BotWarningException("You can only send a message every %d seconds", timeout);
        }

        TextChannel destination = getDestination();

        if (destination == null || !PermissionUtil.checkPermission(destination.getPermissionContainer(),
            getServer().getGuild().getSelfMember(), Permission.MESSAGE_EMBED_LINKS, Permission.MANAGE_THREADS,
            (privateThreads ? Permission.CREATE_PRIVATE_THREADS : Permission.CREATE_PUBLIC_THREADS))) {
            throw new BotErrorException("If you see this error, the server admins messed up");
        }

        timeouts.put(author.getId(), System.currentTimeMillis());
        userCount.put(author.getId(), userCount.getOrDefault(author.getId(), 0) + 1);
        ++threadId;
        getDatabaseManager().setSetting("threadid", String.valueOf(threadId));
        String topic = String.format("t%d-%s", threadId, subject);
        topic = topic.substring(0, Math.min(topic.length(), 25));

        destination.createThreadChannel(topic, privateThreads).queue(thread -> {
            String urls = "";
            if (!attachments.isEmpty()) {
                List<String> list = new ArrayList<>();

                for (Message.Attachment attachment : attachments) {
                    list.add(attachment.getUrl());
                }

                urls = String.join("\n", list.toArray(new String[0]));
            }

            MessageCreateBuilder builder = new MessageCreateBuilder();

            if (mention != null) {
                builder.addContent(mention.getAsMention());
            }

            EmbedBuilder userEmbed = new EmbedBuilder()
                .setColor(Colors.TRANSPARENT)
                .setDescription(String.format("%s `%s`", author.getAsMention(), author.getId()))
                .setFooter(getName(), author.getEffectiveAvatarUrl())
                .setTimestamp(Instant.now());

            EmbedBuilder messageEmbed = new EmbedBuilder()
                .setColor(Colors.TRANSPARENT)
                .setTitle(subject)
                .setDescription(String.format("```%s```", message));

            if (urls.length() > 1) {
                messageEmbed.addField("Attachments", String.format("```%s```", urls), false);
            }

            builder.addEmbeds(userEmbed.build(), messageEmbed.build())
                .addActionRow(
                    Button.secondary("modmail-archive-" + thread.getId(), "Archive ticket")
                        .withEmoji(Emoji.fromUnicode("\uD83D\uDCE5"))
                );

            thread.addThreadMember(author).queue(success ->
                thread.sendMessage(builder.build()).queue()
            );

            reply.send(new MessageCreateBuilder()
                .addEmbeds(new EmbedBuilder()
                    .setColor(Colors.GREEN)
                    .setDescription(Icon.OK + " Message successfully submitted")
                    .build()
                )
                .addActionRow(Button.link(thread.getJumpUrl(), "Go to ticket"))
                .build()
            );
        });

        ++tickets;
    }

    @Override
    protected synchronized void handleDirect(@NotNull Message message, Reply reply) {
        String content = message.getContentRaw();
        synchronized (userCount) {
            synchronized (timeouts) {
                createTicket(message.getAuthor(), message.getAuthor().getAsTag(), content, message.getAttachments(),
                    reply);
            }
        }
    }

    public void stop(Reply reply) {
        super.stop(reply);
        reply.send(Icon.STOP, "Mod mail service stopped");
    }

    public void start(Reply reply) {
        super.start(reply);
        reply.ok("Mod mail service started");
    }

}
