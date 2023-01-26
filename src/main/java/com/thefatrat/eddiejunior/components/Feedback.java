package com.thefatrat.eddiejunior.components;

import com.thefatrat.eddiejunior.Bot;
import com.thefatrat.eddiejunior.entities.Command;
import com.thefatrat.eddiejunior.entities.Interaction;
import com.thefatrat.eddiejunior.exceptions.BotErrorException;
import com.thefatrat.eddiejunior.exceptions.BotWarningException;
import com.thefatrat.eddiejunior.reply.EditReply;
import com.thefatrat.eddiejunior.reply.Reply;
import com.thefatrat.eddiejunior.sources.Server;
import com.thefatrat.eddiejunior.util.Colors;
import com.thefatrat.eddiejunior.util.Icon;
import com.thefatrat.eddiejunior.util.PermissionChecker;
import com.thefatrat.eddiejunior.util.URLUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;

public class Feedback extends DirectComponent {

    public static final String NAME = "Feedback";

    private final Set<String> domains = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<String> filetypes = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<String> users = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final List<Submission> submissions = new CopyOnWriteArrayList<>();
    private int submissionCount = 0;

    public Feedback(Server server) {
        super(server, NAME, false);

        domains.addAll(getDatabaseManager().getSettings("domains"));
        filetypes.addAll(getDatabaseManager().getSettings("filetypes"));

        addSubcommands(
            new Command("raffle", "pick a random submission")
                .setAction((command, reply) -> {
                    if (!isRunning()) {
                        throw new BotWarningException("There is no feedback session at the moment");
                    }
                    if (submissionCount == 0) {
                        throw new BotWarningException("No submissions have been received yet");
                    }
                    if (submissions.size() == 0) {
                        throw new BotWarningException("There are no submissions left in the queue");
                    }

                    TextChannel destination = getDestination();

                    if (destination == null) {
                        throw new BotErrorException("Destination not set yet");
                    }

                    PermissionChecker.requireSend(getDestination());

                    Collections.shuffle(submissions);

                    Submission submission = submissions.get(0);
                    submissions.remove(0);

                    reply.send(Icon.WIN, "%s has won!", submission.user().getAsMention());
                    destination.sendMessage(submission.submission()).queue();
                }),

            new Command("reset", "allow submissions for users again")
                .addOption(new OptionData(OptionType.USER, "user", "user", false))
                .setAction((command, reply) -> {
                    if (!command.getArgs().containsKey("user")) {
                        users.clear();
                        reply.send(Icon.RESET, "Feedback session reset, users can submit again");
                        return;
                    }

                    Member member = command.getArgs().get("user").getAsMember();

                    if (member == null) {
                        throw new BotErrorException("The given member was not found");
                    }

                    users.remove(member.getId());

                    reply.send(Icon.RESET, "Feedback session reset for %s, they can submit again",
                        member.getAsMention());
                }),

            new Command("domains", "manage the domain whitelist for feedback submissions")
                .addOption(new OptionData(OptionType.STRING, "action", "action", true)
                    .addChoice("show", "show")
                    .addChoice("add", "add")
                    .addChoice("remove", "remove")
                    .addChoice("clear", "clear")
                )
                .addOption(new OptionData(OptionType.STRING, "domains",
                    "domains seperated by comma", false))
                .setAction((command, reply) -> {
                    String action = command.getArgs().get("action").getAsString();
                    if (!command.getArgs().containsKey("domains")
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
                            .setTitle(getTitle() + " whitelist")
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
                            .thenRun(() ->
                                reply.ok("Domain whitelist cleared")
                            );
                        return;
                    }

                    boolean add = "add".equals(action);

                    String[] domains = command.getArgs().get("domains").getAsString().toLowerCase().split(", ?");
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
                }),

            new Command("filetypes", "manage discordapp filetypes filter, " +
                "filetypes only work if discordapp.com is whitelisted")
                .addOption(new OptionData(OptionType.STRING, "action", "action", true)
                    .addChoice("show", "show")
                    .addChoice("add", "add")
                    .addChoice("remove", "remove")
                    .addChoice("clear", "clear")
                )
                .addOption(new OptionData(OptionType.STRING, "filetypes",
                    "filetypes seperated by comma", false))
                .setAction((command, reply) -> {
                    String action = command.getArgs().get("action").getAsString();
                    if (!command.getArgs().containsKey("filetypes")
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
                            .setTitle(getTitle() + " filetypes")
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
                            .thenRun(() -> reply.ok("Filetype list cleared"));
                        return;
                    }

                    boolean add = "add".equals(action);

                    String[] filetypes = command.getArgs().get("filetypes").getAsString()
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
                })
        );

        addInteractions(
            new Interaction("mark read")
                .setAction((event, reply) -> {
                    interactionCheck(event.getMessage());

                    MessageEditBuilder builder = MessageEditBuilder.fromMessage(event.getMessage());
                    EmbedBuilder embed = new EmbedBuilder(builder.getEmbeds().get(0));
                    embed.setColor(Colors.TRANSPARENT);
                    builder.setEmbeds(embed.build());
                    builder.setComponents();
                    event.getMessage().editMessage(builder.build()).queue();
                    reply.hide();
                    reply.ok("Submission marked as read");
                }),

            new Interaction("mark unread")
                .setAction((event, reply) -> {
                    interactionCheck(event.getMessage());

                    MessageEditBuilder builder = MessageEditBuilder.fromMessage(event.getMessage());
                    EmbedBuilder embed = new EmbedBuilder(builder.getEmbeds().get(0));
                    embed.setColor(Colors.GREEN);
                    builder.setEmbeds(embed.build());
                    builder.setComponents(ActionRow.of(
                        Button.secondary("feedback-mark_read", "Mark as read").withEmoji(Emoji.fromUnicode("✅"))
                    ));
                    event.getMessage().editMessage(builder.build()).queue();
                    reply.hide();
                    reply.ok("Submission marked as unread");
                })
        );

        getServer().getButtonHandler().addListener((event, reply) -> {
            String buttonId = event.getButtonId();
            if (!buttonId.startsWith("feedback-")) {
                return;
            }

            String[] split = buttonId.split("-");
            String action = split[1];

            if ("mark_read".equals(action)) {
                PermissionChecker.requireSend(
                    event.getMessage().getChannel().asGuildMessageChannel().getPermissionContainer());

                MessageEditBuilder builder = MessageEditBuilder.fromMessage(event.getMessage());
                EmbedBuilder embed = new EmbedBuilder(builder.getEmbeds().get(0));
                embed.setColor(Colors.TRANSPARENT);
                builder.setEmbeds(embed.build());
                builder.setComponents();
                reply.edit(builder.build());
            }
        });
    }

    private void interactionCheck(@NotNull Message message) {
        if (!Bot.getInstance().getJDA().getSelfUser().getId()
            .equals(message.getAuthor().getId())) {
            throw new BotWarningException("Message was not send by me");
        }
        List<MessageEmbed> embeds = message.getEmbeds();
        if (embeds.isEmpty()) {
            throw new BotErrorException("Message does not contain embeds");
        }
        MessageEmbed embed = embeds.get(0);
        if (embed.getFooter() == null || !getName().equals(embed.getFooter().getText())) {
            throw new BotErrorException("Could not perform action");
        }

        PermissionChecker.requireSend(message.getChannel().asGuildMessageChannel().getPermissionContainer());
    }

    @Override
    public String getStatus() {
        String dest = Optional.ofNullable(getDestination())
            .map(Channel::getAsMention)
            .orElse(null);
        return String.format("""
                Enabled: %b
                Running: %b
                Submissions: %d
                Destination: %s
                """,
            isEnabled(), isRunning(), submissionCount, dest);
    }

    @Override
    protected synchronized <T extends Reply & EditReply> void handleDirect(@NotNull Message message, T reply) {
        List<Message.Attachment> attachments = message.getAttachments();

        String url = null;
        Matcher matcher = null;
        if (!attachments.isEmpty()) {
            url = attachments.get(0).getUrl();

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
        if (url == null || matcher == null) {
            throw new BotWarningException("Please send a valid file or link");
        }

        User author = message.getAuthor();

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
            .setColor(Colors.GREEN)
            .setTimestamp(Instant.now())
            .setAuthor(author.getAsTag(), null, author.getEffectiveAvatarUrl())
            .addField("User", String.format("%s `(%s)`", author.getAsMention(), author.getId()), true)
            .addField("Submission", String.format("<%s>", url), true)
            .setFooter(getName());
        if (!URLUtil.isSafe(url)) {
            embed.addField(Icon.WARNING.toString(),
                "The source is not HTTPS and might not be safe",
                true);
        }

        users.add(author.getId());

        builder.setEmbeds(embed.build());
        builder.addActionRow(
            Button.secondary("feedback-mark_read", "Mark as read").withEmoji(Emoji.fromUnicode("✅"))
        );

        submissions.add(new Submission(author, builder.build()));
        submissionCount++;
        reply.edit(new EmbedBuilder()
            .setDescription(Icon.OK + " Successfully submitted")
            .setColor(Icon.OK.getColor())
            .build()
        );

    }

    public void start(Reply reply) {
        super.start(reply);
        users.clear();
        submissions.clear();
        submissionCount = 0;
        reply.ok("Feedback session started");
    }

    public void stop(Reply reply) {
        super.stop(reply);
        submissions.clear();
        submissionCount = 0;
        reply.send(Icon.STOP, "Feedback session stopped");
    }

    private record Submission(User user, MessageCreateData submission) {
    }

}
