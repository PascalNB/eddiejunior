package com.thefatrat.eddiejunior.components.impl;

import com.thefatrat.eddiejunior.entities.Command;
import com.thefatrat.eddiejunior.exceptions.BotErrorException;
import com.thefatrat.eddiejunior.exceptions.BotWarningException;
import com.thefatrat.eddiejunior.reply.MenuReply;
import com.thefatrat.eddiejunior.reply.Reply;
import com.thefatrat.eddiejunior.sources.Server;
import com.thefatrat.eddiejunior.util.Colors;
import com.thefatrat.eddiejunior.util.Icon;
import com.thefatrat.eddiejunior.util.PermissionChecker;
import com.thefatrat.eddiejunior.util.URLUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;

public class FeedbackComponent extends DirectMessageComponent {

    public static final String NAME = "Feedback";

    private final Set<String> domains = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<String> filetypes = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<String> users = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final List<Submission> submissions = new CopyOnWriteArrayList<>();
    private String winChannel;
    private String buttonChannelId;
    private Message buttonMessage = null;
    private int submissionCount = 0;

    public FeedbackComponent(Server server) {
        super(server, NAME, "Song Review (Feedback)", false);

        domains.addAll(getDatabaseManager().getSettings("domains"));
        filetypes.addAll(getDatabaseManager().getSettings("filetypes"));
        winChannel = getDatabaseManager().getSetting("winchannel");
        buttonChannelId = getDatabaseManager().getSetting("buttonchannel");

        addSubcommands(
            new Command("winchannel", "set the channel where win messages will be sent to")
                .addOptions(new OptionData(OptionType.CHANNEL, "channel", "channel", true))
                .setAction((command, reply) -> {
                    TextChannel channel = command.get("channel").getAsChannel().asTextChannel();
                    PermissionChecker.requireSend(channel);

                    winChannel = channel.getId();
                    getDatabaseManager().setSetting("winchannel", winChannel);
                    reply.ok("Set win channel to %s", channel.getAsMention());
                }),

            new Command("removesubmission", "remove a user's submission")
                .addOptions(new OptionData(OptionType.USER, "user", "user", true))
                .setAction((command, reply) -> {
                    User user = command.get("user").getAsUser();
                    String id = user.getId();

                    boolean removed = submissions.removeIf(s -> s.user.getId().equals(id));

                    if (removed) {
                        reply.ok("Removed submission by %s", user.getAsMention());
                    } else {
                        throw new BotWarningException("No submission was removed");
                    }
                }),

            new Command("reset", "allow submissions for users again")
                .addOptions(new OptionData(OptionType.USER, "user", "user", false))
                .setAction((command, reply) -> {
                    if (!command.hasOption("user")) {
                        users.clear();
                        reply.send(Icon.RESET, "Feedback session reset, users can submit again");
                        return;
                    }

                    Member member = command.get("user").getAsMember();

                    if (member == null) {
                        throw new BotErrorException("The given member was not found");
                    }

                    users.remove(member.getId());

                    reply.send(Icon.RESET, "Feedback session reset for %s, they can submit again",
                        member.getAsMention());
                }),

            new Command("domains", "manage the domain whitelist for feedback submissions")
                .addOptions(new OptionData(OptionType.STRING, "action", "action", true)
                    .addChoice("show", "show")
                    .addChoice("add", "add")
                    .addChoice("remove", "remove")
                    .addChoice("clear", "clear")
                )
                .addOptions(new OptionData(OptionType.STRING, "domains",
                    "domains seperated by comma", false))
                .setAction((command, reply) -> {
                    String action = command.get("action").getAsString();
                    if (!command.hasOption("domains")
                        && ("add".equals(action) || "remove".equals(action))) {
                        throw new BotErrorException("Please specify the domains");
                    }

                    if ("show".equals(action)) {
                        if (domains.isEmpty()) {
                            throw new BotWarningException("The domain whitelist is empty");
                        }

                        String[] sorted = new String[domains.size()];
                        int i = 0;
                        for (String s : domains) {
                            sorted[i] = '`' + s + '`';
                            ++i;
                        }
                        Arrays.sort(sorted);

                        reply.send(new EmbedBuilder()
                            .setColor(Colors.TRANSPARENT)
                            .setTitle("Feedback whitelist")
                            .setDescription(String.join("\n", sorted))
                            .build());
                        return;
                    }

                    if ("clear".equals(action)) {
                        if (domains.isEmpty()) {
                            throw new BotWarningException("The domain whitelist is already empty");
                        }

                        domains.clear();
                        getDatabaseManager().removeSetting("domains")
                            .queue(c ->
                                reply.ok("Domain whitelist cleared")
                            );
                        return;
                    }

                    boolean add = "add".equals(action);

                    String[] domains = command.get("domains").getAsString().toLowerCase().split(", ?");
                    for (String domain : domains) {
                        if (!URLUtil.isDomain(domain)) {
                            throw new BotErrorException("`%s` is not a valid domain", domain);
                        }
                    }

                    List<String> changed = new ArrayList<>();

                    String msg;

                    if (add) {
                        msg = "added to";
                        for (String domain : domains) {
                            if (this.domains.contains(domain)) {
                                continue;
                            }
                            getDatabaseManager().addSetting("domains", domain);
                            this.domains.add(domain);
                            changed.add('`' + domain + '`');
                        }
                    } else {
                        msg = "removed from";
                        for (String domain : domains) {
                            if (!this.domains.contains(domain)) {
                                continue;
                            }
                            getDatabaseManager().removeSetting("domains", domain);
                            this.domains.remove(domain);
                            changed.add('`' + domain + '`');
                        }
                    }

                    if (changed.isEmpty()) {
                        return;
                    }

                    reply.ok("Domain%s %s %s the whitelist", changed.size() == 1 ? "" : "s",
                        String.join(", ", changed), msg);
                    getServer().log(Colors.GRAY, command.getMember().getUser(), "Domain%s %s %s the whitelist of `%s`",
                        changed.size() == 1 ? "" : "s", String.join(", ", changed), msg, getId());
                }),

            new Command("filetypes", "manage discordapp filetypes filter, " +
                "filetypes only work if discordapp.com is whitelisted")
                .addOptions(
                    new OptionData(OptionType.STRING, "action", "action", true)
                        .addChoice("show", "show")
                        .addChoice("add", "add")
                        .addChoice("remove", "remove")
                        .addChoice("clear", "clear"),
                    new OptionData(OptionType.STRING, "filetypes", "filetypes seperated by comma", false)
                )
                .setAction((command, reply) -> {
                    String action = command.get("action").getAsString();
                    if (!command.hasOption("filetypes")
                        && ("add".equals(action) || "remove".equals(action))) {
                        throw new BotErrorException("Please specify the filetypes");
                    }

                    if ("show".equals(action)) {
                        if (filetypes.isEmpty()) {
                            throw new BotWarningException("The filetype list is empty");
                        }
                        StringBuilder builder = new StringBuilder();
                        for (String type : filetypes) {
                            builder.append("`").append(type).append("`").append("\n");
                        }
                        builder.deleteCharAt(builder.length() - 1);

                        reply.send(new EmbedBuilder()
                            .setColor(Colors.TRANSPARENT)
                            .setTitle("Feedback filetypes")
                            .setDescription(builder.toString())
                            .build()
                        );
                        return;
                    }

                    if ("clear".equals(action)) {
                        if (filetypes.isEmpty()) {
                            throw new BotWarningException("The filetype list is already empty");
                        }

                        filetypes.clear();
                        getDatabaseManager().removeSetting("filetypes")
                            .queue(c -> reply.ok("Filetype list cleared"));
                        return;
                    }

                    boolean add = "add".equals(action);

                    String[] filetypes = command.get("filetypes").getAsString()
                        .toLowerCase().split(", ?");
                    for (String filetype : filetypes) {
                        if (!filetype.matches("^[a-z\\d]+$")) {
                            throw new BotErrorException("`%s` is not a valid filetype", filetype);
                        }
                    }

                    List<String> changed = new ArrayList<>();

                    String msg;

                    if (add) {
                        msg = "added to";
                        for (String filetype : filetypes) {
                            if (this.filetypes.contains(filetype)) {
                                continue;
                            }
                            getDatabaseManager().addSetting("filetypes", filetype);
                            this.filetypes.add(filetype);
                            changed.add('`' + filetype + '`');
                        }
                    } else {
                        msg = "removed from";
                        for (String filetype : filetypes) {
                            if (!this.filetypes.contains(filetype)) {
                                continue;
                            }
                            getDatabaseManager().removeSetting("filetypes", filetype);
                            this.filetypes.remove(filetype);
                            changed.add('`' + filetype + '`');
                        }
                    }

                    if (changed.isEmpty()) {
                        return;
                    }

                    reply.ok("Filetype%s %s %s the filetype list", changed.size() == 1 ? "" : "s",
                        String.join(", ", changed), msg);
                    getServer().log(Colors.GRAY, command.getMember().getUser(), "Filetype%s %s %s the `%s` " +
                        "filetype list", changed.size() == 1 ? "" : "s", String.join(", ", changed), msg, getId());
                }),

            new Command("button", "set the channel where the submission button will be sent")
                .addOptions(new OptionData(OptionType.CHANNEL, "channel", "channel", true)
                    .setChannelTypes(ChannelType.TEXT)
                )
                .setAction((command, reply) -> {
                    TextChannel channel = command.get("channel").getAsChannel().asTextChannel();
                    PermissionChecker.requireSend(channel);

                    buttonChannelId = channel.getId();
                    reply.ok("Set button channel to %s", channel.getAsMention());
                    getDatabaseManager().setSetting("buttonchannel", buttonChannelId);
                    getServer().log(command.getMember().getUser(), "Set feedback button channel to %s (`%s`)",
                        channel.getAsMention(), channel.getId());
                })
        );

        getServer().getButtonHandler().addListener((event, reply) -> {
            String buttonId = event.getButtonId();
            if (!buttonId.startsWith("feedback-")) {
                return;
            }

            String[] split = buttonId.split("-");
            String action = split[1];

            if ("next".equals(action)) {
                if (!isRunning()) {
                    throw new BotWarningException("There is no feedback session at the moment");
                }
                if (submissionCount == 0) {
                    throw new BotWarningException("No submissions have been received yet");
                }
                if (submissions.size() == 0) {
                    throw new BotWarningException("There are no submissions left in the queue");
                }

                TextChannel channel = event.getMessage().getChannel().asTextChannel();
                PermissionChecker.requireSend(channel);

                Collections.shuffle(submissions);

                Submission submission = submissions.get(0);
                submissions.remove(0);

                if (winChannel != null) {
                    TextChannel output = getGuild().getTextChannelById(winChannel);
                    if (output != null && output.canTalk()) {
                        output.sendMessageEmbeds(
                                new EmbedBuilder()
                                    .setColor(Icon.WIN.getColor())
                                    .setDescription(String.format(
                                        "%s %s has won!",
                                        Icon.WIN, submission.user().getAsMention()
                                    ))
                                    .build()
                            )
                            .queue();
                    }
                }

                channel.sendMessage(submission.submission()).queue();

                MessageEditBuilder builder = MessageEditBuilder.fromMessage(event.getMessage());
                builder.setComponents();
                reply.edit(builder.build());
            } else if ("submit".equals(action)) {
                if (!isRunning()) {
                    throw new BotWarningException("There is no feedback session at the moment");
                }

                String value = "One of the following: " + String.join(", ", domains);

                reply.sendModal(Modal.create("feedback-submit", "Submit song")
                    .addActionRow(TextInput.create("url", "Url", TextInputStyle.PARAGRAPH)
                        .setPlaceholder(value)
                        .setRequiredRange(10, 300)
                        .build())
                    .build());
            }
        });

        getServer().getModalHandler().addListener("feedback-submit", (event, reply) -> {
            String url = event.getValues().get("url").getAsString().trim();
            Matcher matcher = URLUtil.matchUrl(url);
            newSubmission(event.getMember().getUser(), url, matcher);
            reply.hide();
            reply.ok("Successfully submitted");
        });
    }

    private void newSubmission(User author, String url, Matcher matcher) {
        if (url == null || matcher == null) {
            throw new BotWarningException("Please send a valid file or link");
        }

        if (users.contains(author.getId())) {
            throw new BotWarningException("You can only submit once");
        }

        if (!domains.isEmpty()) {
            String domain = matcher.group("domain") + "." + matcher.group("tld");
            if (!domains.contains(domain)) {
                throw new BotWarningException("The server does not accept links from the given source");
            }
            if ("discordapp.com".equals(domain) && !filetypes.isEmpty()) {
                boolean allowed = false;
                for (String filetype : filetypes) {
                    if (url.endsWith("." + filetype)) {
                        allowed = true;
                        break;
                    }
                }
                if (!allowed) {
                    throw new BotWarningException("The server does not accept the given file type");
                }
            }
        }

        MessageCreateBuilder builder = new MessageCreateBuilder();

        EmbedBuilder embed = new EmbedBuilder()
            .setColor(Colors.TRANSPARENT)
            .setTimestamp(Instant.now())
            .setAuthor(author.getEffectiveName(), null, author.getEffectiveAvatarUrl())
            .addField("User", String.format("%s `(%s)`", author.getAsMention(), author.getId()), true)
            .addField("Submission", String.format("<%s>", url), true)
            .setFooter(getId());
        if (!URLUtil.isSafe(url)) {
            embed.addField(Icon.WARNING.toString(),
                "The source is not HTTPS and might not be safe",
                true);
        }

        users.add(author.getId());

        builder.setEmbeds(embed.build());
        builder.addActionRow(
            Button.primary("feedback-next", "Get next song").withEmoji(Emoji.fromUnicode("🎵"))
        );

        submissions.add(new Submission(author, builder.build()));
        submissionCount++;
        getServer().log(Colors.GRAY, author, "Submitted song <%s>", url);
    }

    @Override
    protected synchronized void handleDirect(@NotNull Message message, MenuReply reply) {
        List<Message.Attachment> attachments = message.getAttachments();

        String url = null;
        Matcher matcher = null;
        if (!attachments.isEmpty()) {
            url = attachments.get(0).getUrl();
            matcher = URLUtil.matchUrl(url);
        } else {
            String[] content = message.getContentRaw().split("\\s+");

            for (String part : content) {
                matcher = URLUtil.matchUrl(part);
                if (matcher != null) {
                    url = part;
                    break;
                }
            }
        }

        newSubmission(message.getAuthor(), url, matcher);
        reply.edit(new EmbedBuilder()
            .setDescription(Icon.OK + " Successfully submitted")
            .setColor(Icon.OK.getColor())
            .build());
    }

    public void start(Reply reply) {
        super.start(reply);
        users.clear();
        submissions.clear();
        submissionCount = 0;

        TextChannel destination = getDestination();
        if (destination == null) {
            reply.send(new BotErrorException("Could not start feedback session: destination channel not set"));
            stop(Reply.EMPTY);
            return;
        }

        destination.sendMessage(
                new MessageCreateBuilder()
                    .setEmbeds(
                        new EmbedBuilder()
                            .setColor(Colors.TRANSPARENT)
                            .setTitle("Feedback session started")
                            .build()
                    )
                    .setActionRow(
                        Button.primary("feedback-next", "Get first song").withEmoji(Emoji.fromUnicode("🎵"))
                    )
                    .build()
            )
            .queue();

        if (buttonChannelId != null) {
            TextChannel buttonChannel = getGuild().getTextChannelById(buttonChannelId);
            if (buttonChannel != null && buttonChannel.canTalk()) {
                buttonChannel.sendMessage(new MessageCreateBuilder()
                        .setContent("Use the button below to submit a song.")
                        .addActionRow(Button.success("feedback-submit", "Submit song")
                            .withEmoji(Emoji.fromUnicode("🎵")))
                        .build())
                    .queue(message -> {
                        buttonMessage = message;
                        try {
                            message.pin().onErrorMap(e -> null).queue();
                        } catch (InsufficientPermissionException ignore) {
                        }
                    });
            }
        }

        reply.ok("Feedback session started");
    }

    public void stop(Reply reply) {
        super.stop(reply);
        submissions.clear();
        submissionCount = 0;

        if (buttonMessage != null) {
            buttonMessage.delete().onErrorMap(e -> null).queue();
        }

        reply.send(Icon.STOP, "Feedback session stopped");
    }

    @Override
    public String getStatus() {
        String dest = Optional.ofNullable(getDestination())
            .map(Channel::getAsMention)
            .orElse(null);
        String win = Optional.ofNullable(winChannel)
            .map(getGuild()::getTextChannelById)
            .map(Channel::getAsMention)
            .orElse(null);
        String butt = Optional.ofNullable(buttonChannelId)
            .map(getGuild()::getTextChannelById)
            .map(Channel::getAsMention)
            .orElse(null);
        return String.format("""
                Enabled: %b
                Running: %b
                Submissions: %d
                Destination: %s
                Win channel: %s
                Button channel: %s
                """,
            isEnabled(), isRunning(), submissionCount, dest, win, butt);
    }

    private record Submission(User user, MessageCreateData submission) {
    }

}
