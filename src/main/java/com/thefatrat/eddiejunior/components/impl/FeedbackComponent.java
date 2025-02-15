package com.thefatrat.eddiejunior.components.impl;

import com.thefatrat.eddiejunior.entities.Command;
import com.thefatrat.eddiejunior.entities.UserRole;
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
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
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
    private String voiceChannel;
    private String buttonChannelId;
    private Message buttonMessage = null;
    private int submissionCount = 0;
    private Role winRole = null;
    private Member currentWinner = null;
    private boolean autoStageInvite;

    public FeedbackComponent(Server server) {
        super(server, NAME, "Song Review (Feedback)", false);

        domains.addAll(getDatabaseManager().getSettings("domains"));
        filetypes.addAll(getDatabaseManager().getSettings("filetypes"));
        winChannel = getDatabaseManager().getSetting("winchannel");
        voiceChannel = getDatabaseManager().getSetting("voicechannel");
        buttonChannelId = getDatabaseManager().getSetting("buttonchannel");
        autoStageInvite = getDatabaseManager().getSettingOrDefault("autoinvite", false);
        String winRoleId = getDatabaseManager().getSetting("winRole");
        if (winRoleId != null) {
            winRole = getGuild().getRoleById(winRoleId);
        }

        addSubcommands(
            new Command("winchannel", "set the channel where win messages will be sent to")
                .addOptions(new OptionData(OptionType.CHANNEL, "channel", "channel", true))
                .setAction((command, reply) -> {
                    TextChannel channel = command.get("channel").getAsChannel().asTextChannel();
                    PermissionChecker.requireSend(channel);

                    winChannel = channel.getId();
                    getDatabaseManager().setSetting("winchannel", winChannel);
                    reply.ok("Set win channel to %s", channel.getAsMention());
                    getServer().log(command.getMember().getUser(), "Set feedback win channel to %s (`%s`)",
                        channel.getAsMention(), channel.getId());
                }),

            new Command("winrole", "set the role that is given to the current winner")
                .addOptions(new OptionData(OptionType.ROLE, "role", "role", true))
                .setAction((command, reply) -> {
                    Role role = command.get("role").getAsRole();
                    if (!getGuild().getSelfMember().canInteract(role)) {
                        throw new BotWarningException("Cannot interact with %s", role.getAsMention());
                    }

                    this.winRole = role;
                    getDatabaseManager().setSetting("winrole", winRole.getId());
                    reply.ok("Set winning role to %s", role.getAsMention());
                    getServer().log(command.getMember().getUser(), "Set feedback winning role to %s (`%s`)",
                        role.getAsMention(), role.getId());
                }),

            new Command("removesubmission", "remove a user's submission")
                .setRequiredUserRole(UserRole.USE)
                .addOptions(
                    new OptionData(OptionType.USER, "user", "user", true),
                    new OptionData(OptionType.BOOLEAN, "reset", "let user submit again", true)
                )
                .setAction((command, reply) -> {
                    if (!isRunning()) {
                        throw new BotWarningException("There is no ongoing feedback session at the moment");
                    }

                    User user = command.get("user").getAsUser();
                    String id = user.getId();
                    boolean reset = command.get("reset").getAsBoolean();

                    boolean removed = submissions.removeIf(s -> s.member().getId().equals(id));

                    if (removed) {
                        if (reset) {
                            users.remove(id);
                            reply.ok("Removed submission by %s, they can submit again", user.getAsMention());
                        } else {
                            reply.ok("Removed submission by %s", user.getAsMention());
                        }
                        getServer().log(Colors.RED, command.getMember().getUser(), "Removed submission by %s (`%s`)",
                            user.getAsMention(), id);
                        return;
                    }

                    throw new BotWarningException("No submission was removed");
                }),

            new Command("reset", "allow submissions for users again")
                .setRequiredUserRole(UserRole.USE)
                .addOptions(new OptionData(OptionType.USER, "user", "user", false))
                .setAction((command, reply) -> {
                    if (!isRunning()) {
                        throw new BotWarningException("There is no ongoing feedback session at the moment");
                    }

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
                            .async(c ->
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
                            .async(c -> reply.ok("Filetype list cleared"));
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
                }),

            new Command("list", "list all feedback submissions")
                .setAction((command, reply) -> {
                    if (submissions.isEmpty()) {
                        throw new BotErrorException("There are no feedback submissions currently in queue");
                    }

                    StringBuilder builder = new StringBuilder();

                    for (Submission submission : submissions) {
                        builder.append(submission.member().getAsMention()).append("\n");
                    }

                    builder.deleteCharAt(builder.length() - 1);

                    reply.hide();
                    reply.send(builder.toString());
                }),

            new Command("voicechannel", "set voice channel that users need to be connected to")
                .addOptions(
                    new OptionData(OptionType.CHANNEL, "channel", "channel", false)
                        .setChannelTypes(ChannelType.VOICE, ChannelType.STAGE)
                )
                .setAction((command, reply) -> {
                    if (command.hasOption("channel")) {
                        AudioChannel audioChannel = command.get("channel").getAsChannel().asAudioChannel();
                        voiceChannel = audioChannel.getId();
                        getDatabaseManager().setSetting("voicechannel", voiceChannel);
                        reply.ok("Set voice channel to %s", audioChannel.getAsMention());
                        getServer().log(Colors.GRAY, command.getMember().getUser(),
                            "Set feedback voice channel to %s (`%s`)",
                            audioChannel.getAsMention(), audioChannel.getId());
                    } else {
                        voiceChannel = null;
                        getDatabaseManager().removeSetting("voicechannel");
                        reply.ok("Removed voice channel");
                        getServer().log(Colors.GRAY, command.getMember().getUser(),
                            "Removed feedback voice channel");
                    }
                }),

            new Command("autoinvite", "set whether users should be invited to stage if they win")
                .addOptions(
                    new OptionData(OptionType.BOOLEAN, "autoinvite", "autoinvite", true)
                )
                .setAction((command, reply) -> {
                    boolean autoinvite = command.get("autoinvite").getAsBoolean();
                    this.autoStageInvite = autoinvite;
                    getDatabaseManager().setSetting("autoinvite", autoinvite);
                    reply.ok("Set auto invite to `%s`", autoinvite);
                    getServer().log(command.getMember().getUser(), "Set auto invite to `%s`", autoinvite);
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
                if (submissions.isEmpty()) {
                    throw new BotWarningException("There are no submissions left in the queue");
                }

                TextChannel submissionChannel = event.getMessage().getChannel().asTextChannel();
                PermissionChecker.requireSend(submissionChannel);
                Submission submission = null;

                AudioChannel audioChannel = voiceChannel == null ? null :
                    getGuild().getChannelById(AudioChannel.class, voiceChannel);

                GuildVoiceState voiceState = null;

                while (!submissions.isEmpty()) {
                    Collections.shuffle(submissions);
                    submission = submissions.remove(0);

                    if (audioChannel == null) {
                        break;
                    }

                    voiceState = getGuild().retrieveMemberVoiceState(submission.member())
                        .onErrorMap(e -> null)
                        .complete();

                    if (voiceState != null) {
                        AudioChannel connected = voiceState.getChannel();
                        if (connected != null && connected.getId().equals(voiceChannel)) {
                            break;
                        }
                    }

                    getServer().log(Colors.GRAY, submission.member().getUser(),
                        "Skipped song because of inactivity");

                    submission = null;
                }

                if (submission == null) {
                    throw new BotWarningException("No users were connected to the voice channel");
                }

                if (currentWinner != null && winRole != null) {
                    if (!currentWinner.equals(submission.member())) {
                        try {
                            getGuild().removeRoleFromMember(currentWinner, winRole).onErrorMap(__ -> null).queue();
                        } catch (Exception ignore) {
                        }
                    }
                }
                currentWinner = submission.member();
                if (winRole != null) {
                    try {
                        getGuild().addRoleToMember(currentWinner, winRole).onErrorMap(__ -> null).queue();
                    } catch (Exception ignore) {
                    }
                }

                if (autoStageInvite && voiceState != null && voiceState.getChannel() != null
                    && voiceState.getChannel().getType().equals(ChannelType.STAGE)) {
                    try {
                        voiceState.inviteSpeaker().queue();
                    } catch (Exception ignore) {
                    }
                }

                if (winChannel != null) {
                    TextChannel output = getGuild().getTextChannelById(winChannel);
                    if (output != null && output.canTalk()) {
                        output.sendMessage(new MessageCreateBuilder()
                                .setContent(submission.member().getAsMention())
                                .addEmbeds(new EmbedBuilder()
                                    .setColor(Icon.WIN.getColor())
                                    .setDescription(String.format("%s You won!", Icon.WIN))
                                    .build()
                                )
                                .build()
                            )
                            .queue();
                    }
                }

                submissionChannel.sendMessage(submission.submission()).queue();

                MessageEditBuilder builder = MessageEditBuilder.fromMessage(event.getMessage());
                List<Button> buttons = builder.getComponents()
                    .stream()
                    .flatMap(l -> l.getButtons().stream())
                    .filter(b -> b.getStyle().equals(ButtonStyle.DANGER))
                    .toList();
                if (buttons.isEmpty()) {
                    builder.setComponents();
                } else {
                    builder.setComponents(ActionRow.of(buttons));
                }
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
            } else if ("stop".equals(action)) {
                if (!isRunning()) {
                    throw new BotWarningException("There is no feedback session running at the moment");
                }

                this.stop(reply);
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

        Member member = getGuild().retrieveMember(author).complete();

        if (voiceChannel != null) {
            AudioChannel audioChannel = getGuild().getChannelById(AudioChannel.class, voiceChannel);
            if (audioChannel != null) {
                GuildVoiceState voiceState = getGuild().retrieveMemberVoiceState(member)
                    .onErrorMap(__ -> null)
                    .complete();
                if (voiceState != null) {
                    AudioChannel connected = voiceState.getChannel();
                    if (connected == null || !connected.getId().equals(voiceChannel)) {
                        throw new BotWarningException("You are not connected to the voice channel");
                    }
                } else {
                    throw new BotWarningException("You are not connected to the voice channel");
                }
            } else {
                voiceChannel = null;
                getDatabaseManager().removeSetting("voicechannel");
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

        submissions.add(new Submission(member, builder.build()));
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
                        Button.primary("feedback-next", "Get first song").withEmoji(Emoji.fromUnicode("🎵")),
                        Button.danger("feedback-stop", "End the feedback session").withEmoji(Emoji.fromUnicode("✖️"))
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

        if (currentWinner != null) {
            if (winRole != null) {
                try {
                    getGuild().removeRoleFromMember(currentWinner, winRole).onErrorMap(__ -> null).queue();
                } catch (InsufficientPermissionException ignore) {
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            currentWinner = null;
        }

        this.clearRequests();

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
        String voice = Optional.ofNullable(voiceChannel)
            .map(getGuild()::getGuildChannelById)
            .map(Channel::getAsMention)
            .orElse(null);
        return String.format("""
                Enabled: %b
                Running: %b
                Submissions: %d
                Submissions in queue: %d
                Destination: %s
                Win channel: %s
                Win role: %s
                Button channel: %s
                Voice channel: %s
                Auto invite: %s
                """,
            isEnabled(), isRunning(), submissionCount, submissions.size(), dest, win, winRole, butt, voice,
            autoStageInvite);
    }

    private record Submission(Member member, MessageCreateData submission) {
    }

}
