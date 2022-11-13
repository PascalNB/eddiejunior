package com.thefatrat.application.components;

import com.thefatrat.application.Bot;
import com.thefatrat.application.entities.Command;
import com.thefatrat.application.entities.Interaction;
import com.thefatrat.application.entities.Reply;
import com.thefatrat.application.exceptions.BotErrorException;
import com.thefatrat.application.exceptions.BotWarningException;
import com.thefatrat.application.sources.Server;
import com.thefatrat.application.util.Colors;
import com.thefatrat.application.util.URLChecker;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.internal.utils.PermissionUtil;

import java.net.URISyntaxException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

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

                    if (!getDestination().canTalk() ||
                        !PermissionUtil.checkPermission(getDestination().getPermissionContainer(),
                            getServer().getGuild().retrieveMember(Bot.getInstance().getJDA().getSelfUser()).complete(),
                            Permission.MESSAGE_EMBED_LINKS)) {

                        throw new BotErrorException("Could not send submission to destination channel");
                    }

                    Collections.shuffle(submissions);

                    Submission submission = submissions.get(0);
                    submissions.remove(0);

                    MessageEmbed embed = new EmbedBuilder()
                        .setColor(Colors.BLUE)
                        .setDescription(String.format("%s has won!", submission.user())).build();

                    getDestination().sendMessageEmbeds(submission.submission()).queue(m -> reply.sendEmbed(embed));
                }),

            new Command("reset", "allow submissions for users again")
                .addOption(new OptionData(OptionType.USER, "user", "user", false))
                .setAction((command, reply) -> {
                    if (!command.getArgs().containsKey("user")) {
                        users.clear();
                        reply.sendEmbedFormat(Colors.GRAY,
                            ":arrows_counterclockwise: Feedback session reset, users can submit again"
                        );
                        return;
                    }

                    Member member = command.getArgs().get("user").getAsMember();

                    if (member == null) {
                        reply.sendEmbedFormat(Colors.RED, new BotErrorException(
                            "The given member was not found").getMessage());
                        return;
                    }

                    users.remove(member.getId());

                    reply.sendEmbedFormat(Colors.GRAY,
                        ":arrows_counterclockwise: Feedback session reset for %s, " +
                            "they can submit again", member.getAsMention());
                }),

            new Command("domains", "manage the domain whitelist for feedback submissions")
                .addOption(new OptionData(OptionType.STRING, "action", "action", true)
                    .addChoice("add", "add")
                    .addChoice("remove", "remove")
                    .addChoice("clear", "clear")
                    .addChoice("show", "show")
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
                        StringBuilder builder = new StringBuilder();

                        String[] sorted = new String[domains.size()];
                        int i = 0;
                        for (String s : domains) {
                            sorted[i] = s;
                            ++i;
                        }
                        Arrays.sort(sorted);

                        for (String domain : sorted) {
                            builder.append("`").append(domain).append("`\n");
                        }
                        builder.deleteCharAt(builder.length() - 1);

                        reply.sendEmbed(new EmbedBuilder()
                            .setColor(Colors.WHITE)
                            .addField("Whitelist", builder.toString(), false)
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
                                reply.sendEmbedFormat(Colors.GREEN, ":white_check_mark: Domain whitelist " +
                                    "cleared")
                            )
                            .join();
                        return;
                    }

                    boolean add = "add".equals(action);

                    String[] domains = command.getArgs().get("domains").getAsString().toLowerCase().split(", ?");
                    for (String domain : domains) {
                        if (!URLChecker.isDomain(domain)) {
                            throw new BotErrorException(String.format(
                                "`%s` is not a valid domain", domain));
                        }
                    }

                    List<String> changed = new ArrayList<>();

                    String msg;

                    if (add) {
                        msg = "added to";
                        for (String domain : domains) {
                            if (this.domains.contains(domain)) {
                                reply.sendEmbedFormat(Colors.YELLOW,
                                    new BotWarningException(String.format(
                                        "Domain %s is already in the domain whitelist", domain))
                                        .getMessage()
                                );
                                continue;
                            }
                            getDatabaseManager().addSetting("domains", domain).join();
                            this.domains.add(domain);
                            changed.add(domain);
                        }
                    } else {
                        msg = "removed from";
                        for (String domain : domains) {
                            if (!this.domains.contains(domain)) {
                                reply.sendEmbedFormat(Colors.YELLOW,
                                    new BotWarningException(String.format(
                                        "Domain %s is not in the domain whitelist", domain))
                                        .getMessage()
                                );
                                continue;
                            }
                            getDatabaseManager().removeSetting("domains", domain).join();
                            this.domains.remove(domain);
                            changed.add(domain);
                        }
                    }

                    if (changed.isEmpty()) {
                        return;
                    }

                    reply.sendEmbedFormat(Colors.GREEN, ":white_check_mark: " +
                            "Domain%s %s\n%s the whitelist", changed.size() == 1 ? "" : "s",
                        concatObjects(changed.toArray(), s -> "\n`" + s + "`"), msg);
                }),

            new Command("filetypes", "manage discordapp filetypes filter, " +
                "filetypes only work if discordapp.com is whitelisted")
                .addOption(new OptionData(OptionType.STRING, "action", "action", true)
                    .addChoice("add", "add")
                    .addChoice("remove", "remove")
                    .addChoice("show", "show")
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

                        reply.sendEmbed(new EmbedBuilder()
                            .setColor(Colors.WHITE)
                            .addField("Filetypes", builder.toString(), false)
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
                            .thenRun(() -> reply.sendEmbedFormat(Colors.GREEN,
                                ":white_check_mark: Filetype list cleared"))
                            .join();
                        return;
                    }

                    boolean add = "add".equals(action);

                    String[] filetypes = command.getArgs().get("filetypes").getAsString()
                        .toLowerCase().split(", ?");
                    for (String filetype : filetypes) {
                        if (!filetype.matches("^[a-z\\d]+$")) {
                            throw new BotErrorException(String.format(
                                "`%s` is not a valid filetype", filetype));
                        }
                    }

                    List<String> changed = new ArrayList<>();

                    String msg;

                    if (add) {
                        msg = "added to";
                        for (String filetype : filetypes) {
                            if (this.filetypes.contains(filetype)) {
                                reply.sendEmbedFormat(Colors.YELLOW,
                                    new BotWarningException(String.format(
                                        "Filetype %s is already in the filetype list", filetype))
                                        .getMessage()
                                );
                                continue;
                            }
                            getDatabaseManager().addSetting("filetypes", filetype).join();
                            this.filetypes.add(filetype);
                            changed.add(filetype);
                        }
                    } else {
                        msg = "removed from";
                        for (String filetype : filetypes) {
                            if (!this.filetypes.contains(filetype)) {
                                reply.sendEmbedFormat(Colors.YELLOW,
                                    new BotWarningException(String.format(
                                        "Filetype %s is not in the filetype list", filetype))
                                        .getMessage()
                                );
                                continue;
                            }
                            getDatabaseManager().removeSetting("filetypes", filetype).join();
                            this.filetypes.remove(filetype);
                            changed.add(filetype);
                        }
                    }

                    if (changed.isEmpty()) {
                        return;
                    }

                    reply.sendEmbedFormat(Colors.GREEN, ":white_check_mark: " +
                            "Filetype%s %s\n%s the filetype list", changed.size() == 1 ? "" : "s",
                        concatObjects(changed.toArray(), s -> "\n`" + s + "`"), msg);
                })
        );

        addInteractions(
            new Interaction("mark read")
                .setAction((event, reply) -> {
                    MessageEmbed embed = event.getMessage().getEmbeds().get(0);
                    if (embed.getColorRaw() != Colors.LIGHT) {
                        throw new BotErrorException("Could not perform action");
                    }
                    event.getMessage().editMessageEmbeds(
                        new EmbedBuilder(embed)
                            .setColor(Colors.DARK)
                            .build()
                    ).queue();
                    reply.sendEmbedFormat(Colors.GREEN, ":white_check_mark: `%s` performed successfully",
                        event.getAction());
                }),

            new Interaction("mark unread")
                .setAction((event, reply) -> {
                    MessageEmbed embed = event.getMessage().getEmbeds().get(0);
                    if (embed.getColorRaw() != Colors.DARK) {
                        throw new BotErrorException("Could not perform action");
                    }
                    event.getMessage().editMessageEmbeds(
                        new EmbedBuilder(embed)
                            .setColor(Colors.LIGHT)
                            .build()
                    ).queue();
                    reply.sendEmbedFormat(Colors.GREEN, ":white_check_mark: `%s` performed successfully",
                        event.getAction());
                })
        );
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
    protected synchronized void handleDirect(Message message, Reply reply) {
        List<Message.Attachment> attachments = message.getAttachments();

        String url = null;
        if (!attachments.isEmpty()) {
            url = attachments.get(0).getUrl();

        } else {
            String[] content = message.getContentRaw().split("\\s+");

            for (String part : content) {
                if (URLChecker.isUrl(part)) {
                    url = part;
                    break;
                }
            }
        }
        if (url == null) {
            throw new BotWarningException("Please send a valid file or link");
        }

        User author = message.getAuthor();

        if (users.contains(author.getId())) {
            throw new BotWarningException("You can only submit once");
        }

        if (!domains.isEmpty()) {
            try {
                String domain = URLChecker.isFromDomains(url, domains);
                if (domain == null) {
                    throw new BotWarningException(
                        "The server does not accept links from the given source");
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
                        throw new BotWarningException(
                            "The server does not accept the given file type");
                    }
                }
            } catch (URISyntaxException e) {
                throw new BotWarningException("Please send a valid file or link");
            }
        }

        EmbedBuilder embed = new EmbedBuilder()
            .setColor(Colors.LIGHT)
            .setTimestamp(Instant.now())
            .setAuthor(author.getAsTag(), null, author.getEffectiveAvatarUrl())
            .addField("User", String.format("%s `(%s)`", author.getAsMention(), author.getId()), true)
            .addField("Submission", String.format("<%s>", url), true)
            .setFooter(getName());
        if (!URLChecker.isSafe(url)) {
            embed.addField(BotWarningException.icon,
                "The source is not HTTPS and might not be safe",
                true);
        }

        users.add(author.getId());

        submissions.add(new Submission(author.getAsMention(), embed.build()));
        submissionCount++;
        reply.sendEmbedFormat(Colors.GREEN, ":white_check_mark: Successfully submitted");
    }

    protected void start(Reply reply) {
        super.start(reply);
        users.clear();
        submissions.clear();
        submissionCount = 0;
        reply.sendEmbedFormat(Colors.GREEN, ":white_check_mark: Feedback session started");
    }

    protected void stop(Reply reply) {
        super.stop(reply);
        submissions.clear();
        submissionCount = 0;
        reply.sendEmbedFormat(Colors.GREEN, ":stop_sign: Feedback session stopped");
    }

    private record Submission(String user, MessageEmbed submission) {

    }

}
