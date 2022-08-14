package com.thefatrat.application.components;

import com.thefatrat.application.exceptions.BotErrorException;
import com.thefatrat.application.exceptions.BotWarningException;
import com.thefatrat.application.sources.Server;
import com.thefatrat.application.util.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Channel;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.net.URISyntaxException;
import java.util.*;

public class Feedback extends DirectComponent {

    public static final String NAME = "Feedback";

    private final Set<String> domains = new HashSet<>();
    private final Set<String> filetypes = new HashSet<>();
    private final Set<String> users = new HashSet<>();
    private int submissions = 0;

    public Feedback(Server server) {
        super(server, NAME);

        new Thread(() -> {
            domains.addAll(getDatabaseManager().getSettings("domains"));
            filetypes.addAll(getDatabaseManager().getSettings("filetypes"));
        }).start();

        addSubcommands(
            new Command("reset", "allow submissions for users again")
                .addOption(new OptionData(OptionType.STRING, "member", "member id"))
                .setAction((command, reply) -> {
                    if (!command.getArgs().containsKey("member")) {
                        users.clear();
                        reply.sendEmbedFormat(Colors.GRAY,
                            ":arrows_counterclockwise: Feedback session reset, users can submit again"
                        );
                        return;
                    }

                    long id;
                    try {
                        id = command.getArgs().get("member").getAsLong();
                    } catch (NumberFormatException e) {
                        throw new BotErrorException("Not a valid member id");
                    }

                    command.getGuild().retrieveMemberById(id)
                        .onErrorMap(e -> null)
                        .queue(member -> {
                            if (member == null) {
                                reply.sendEmbedFormat(Colors.RED, new BotErrorException(
                                    "The given member was not found").getMessage());
                                return;
                            }

                            users.remove(member.getId());

                            reply.sendEmbedFormat(Colors.GRAY,
                                ":arrows_counterclockwise: Feedback session reset for %s, " +
                                    "they can submit again", member.getAsMention());
                        });
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
                        for (String domain : domains) {
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
                        getDatabaseManager().removeSetting("domains");
                        reply.sendEmbedFormat(Colors.GREEN, ":white_check_mark: Domain whitelist " +
                            "cleared");
                        return;
                    }

                    boolean add = "add".equals(action);

                    String[] domains = command.getArgs().get("domains").getAsString()
                        .toLowerCase().split(", ?");
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
                            getDatabaseManager().setSetting("domains", domain);
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
                            getDatabaseManager().removeSetting("domains", domain);
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
                        getDatabaseManager().removeSetting("filetypes");
                        reply.sendEmbedFormat(Colors.GREEN,
                            ":white_check_mark: Filetype list cleared");
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
                            getDatabaseManager().setSetting("filetypes", filetype);
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
                            getDatabaseManager().removeSetting("filetypes", filetype);
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
    }

    @Override
    public int getColor() {
        return 0x308acb;
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
            isEnabled(), isRunning() && !isPaused(), submissions, dest);
    }

    @Override
    protected void handleDirect(Message message, Reply reply) {
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

        users.add(author.getId());
        getDestination().sendMessageFormat("%s `(%s)`:%n<%s>",
            author.getAsMention(), author.getId(), url).queue();
        submissions++;
        reply.sendEmbedFormat(Colors.GREEN, ":white_check_mark: Successfully submitted");
    }

    @Override
    protected void start(CommandEvent command, Reply reply) {
        super.start(command, reply);
        users.clear();
        submissions = 0;
        reply.sendEmbedFormat(Colors.GREEN, ":white_check_mark: Feedback session started");
    }

    @Override
    protected void stop(CommandEvent command, Reply reply) {
        super.stop(command, reply);
        submissions = 0;
        reply.sendEmbedFormat(Colors.GREEN, ":stop_sign: Feedback session stopped");
    }

}
