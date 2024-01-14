package com.thefatrat.eddiejunior.components.impl;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.thefatrat.eddiejunior.components.AbstractComponent;
import com.thefatrat.eddiejunior.entities.Command;
import com.thefatrat.eddiejunior.entities.Interaction;
import com.thefatrat.eddiejunior.entities.PermissionEntity;
import com.thefatrat.eddiejunior.events.ButtonEvent;
import com.thefatrat.eddiejunior.events.CommandEvent;
import com.thefatrat.eddiejunior.exceptions.BotErrorException;
import com.thefatrat.eddiejunior.exceptions.BotException;
import com.thefatrat.eddiejunior.exceptions.BotWarningException;
import com.thefatrat.eddiejunior.reply.InteractionReply;
import com.thefatrat.eddiejunior.reply.MenuReply;
import com.thefatrat.eddiejunior.sources.Server;
import com.thefatrat.eddiejunior.util.Colors;
import com.thefatrat.eddiejunior.util.EmojiUtil;
import com.thefatrat.eddiejunior.util.PermissionChecker;
import com.thefatrat.eddiejunior.util.URLUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PollComponent extends AbstractComponent {

    private final Map<String, Poll> polls = new HashMap<>();
    private final ScheduledExecutorService executorService;

    public PollComponent(Server server) {
        super(server, "Poll");

        polls.putAll(getDatabaseManager().getPolls());
        LocalDateTime now = LocalDateTime.now();
        executorService = Executors.newScheduledThreadPool(1);
        for (Poll poll : polls.values()) {
            if (poll.expiry == null) {
                continue;
            }

            Message message = URLUtil.getMessageFromString(poll.channelId + "_" + poll.id, getGuild());
            if (message == null) {
                getDatabaseManager().removePoll(poll);
                return;
            }

            Duration duration;

            if (now.isAfter(poll.expiry)) {
                duration = Duration.ZERO;
            } else {
                duration = Duration.between(now, poll.expiry);
            }

            executorService.schedule(() -> {
                if (polls.containsKey(poll.id)) {
                    this.closePoll(message, null);
                }
            }, duration.getSeconds(), TimeUnit.SECONDS);
        }

        setComponentCommand(PermissionEntity.RequiredPermission.MANAGE);

        addSubcommands(
            new Command("new", "create a new poll")
                .addOptions(
                    new OptionData(OptionType.STRING, "text", "text", true)
                        .setMaxLength(1000),
                    new OptionData(OptionType.STRING, "options", "options seperated by commas", true),
                    new OptionData(OptionType.STRING, "expiry", "time the poll expires in (?d ?h ?m ?s)", false),
                    new OptionData(OptionType.INTEGER, "picks", "maximum number of options a user can choose", false)
                        .setMinValue(1)
                        .setMaxValue(100),
                    new OptionData(OptionType.CHANNEL, "channel", "channel that the poll will be sent in", false)
                        .setChannelTypes(ChannelType.TEXT),
                    new OptionData(OptionType.STRING, "image", "image url", false)
                )
                .setAction(this::newPoll),

            new Command("close", "close poll and show results")
                .addOptions(new OptionData(OptionType.STRING, "url", "message url", true))
                .setAction(this::getPollResults),

            new Command("peek", "show current poll results without closing the poll")
                .setRequiredPermission(PermissionEntity.RequiredPermission.USE)
                .addOptions(new OptionData(OptionType.STRING, "url", "message url", true))
                .setAction(this::getPollInfo)
        );

        getServer().getButtonHandler().addListener(this::castVote);

        addMessageInteractions(
            new Interaction<Message>("close")
                .setRequiredPermission(PermissionEntity.RequiredPermission.MANAGE)
                .setAction((e, r) -> closePoll(e.getEntity(), r)),
            new Interaction<Message>("peek")
                .setRequiredPermission(PermissionEntity.RequiredPermission.USE)
                .setAction((e, r) -> showPoll(e.getEntity(), r))
        );
    }

    /**
     * Sends a new message with a new poll with the given options.
     *
     * @param command command
     * @param reply   reply
     */
    private void newPoll(CommandEvent command, InteractionReply reply) {
        MessageCreateBuilder builder = new MessageCreateBuilder();
        String text = unescape(command.get("text").getAsString());
        final String image;

        if (command.hasOption("image")) {
            image = command.get("image").getAsString();
            if (URLUtil.matchUrl(image) == null) {
                throw new BotWarningException("Please specify a valid image url");
            }
        } else {
            image = null;
        }

        builder.addEmbeds(new EmbedBuilder()
            .setDescription(text)
            .setColor(Colors.TRANSPARENT)
            .setImage(image)
            .build()
        );

        Consumer<Message> consumer = message -> {
            try {
                Duration duration = null;
                if (command.hasOption("expiry")) {
                    String timeString = command.get("expiry").getAsString();
                    duration = formatTime(timeString);
                    if (duration == null) {
                        throw new BotErrorException("Invalid duration: `%s", timeString);
                    }
                }

                this.createPoll(message, command, duration);
            } catch (BotException e) {
                message.editMessageEmbeds(new EmbedBuilder()
                        .setColor(e.getColor())
                        .setDescription(e.getMessage())
                        .build()
                    )
                    .queue();
            }
        };

        Optional.ofNullable(command.get("channel"))
            .ifPresentOrElse(
                object -> {
                    TextChannel channel = object.getAsChannel().asTextChannel();
                    PermissionChecker.requireSend(channel);
                    channel.sendMessage(builder.build()).queue(consumer);
                    reply.hide();
                    reply.ok("Message sent in %s", channel.getAsMention());
                },
                () -> reply.send(builder.build(), consumer)
            );
    }

    /**
     * Creates a new poll with the given options for the given message.
     *
     * @param message message
     * @param command command
     * @throws BotException when something went wrong
     */
    private void createPoll(Message message, CommandEvent command, Duration expiry) throws BotException {
        MessageEditBuilder edit = MessageEditBuilder.fromMessage(message);
        List<Button> buttons = new ArrayList<>();
        Set<String> ids = new HashSet<>();

        String[] options = command.get("options").getAsString().split(", *");

        for (String option : options) {
            if (option.isBlank()) {
                continue;
            }
            if (option.length() > Button.LABEL_MAX_LENGTH) {
                throw new BotWarningException("Option `%s` is too long", option);
            }

            Button temp = EmojiUtil.formatButton("p", option, ButtonStyle.SECONDARY);
            String label;
            if ("".equals(temp.getLabel())) {
                if (Objects.requireNonNull(temp.getEmoji()).getType().equals(Emoji.Type.UNICODE)) {
                    String codepoints = temp.getEmoji().asUnicode().getAsCodepoints();

                    label = Arrays.stream(codepoints.split("U\\+"))
                        .map(s -> {
                            try {
                                return Integer.parseInt(s, 16);
                            } catch (NumberFormatException e) {
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .filter(Character::isValidCodePoint)
                        .map(Character::toString)
                        .collect(Collectors.joining());
                } else {
                    label = temp.getEmoji().asCustom().getFormatted();
                }

            } else {
                label = temp.getLabel();
            }
            String id = message.getId() + "-" + label;

            if (id.length() > Button.ID_MAX_LENGTH) {
                throw new BotWarningException("Option `%s` is too long", option);
            }

            if (!ids.add(id)) {
                throw new BotWarningException("All poll options should be unique");
            }

            buttons.add(
                EmojiUtil.formatButton("poll-" + id, option, ButtonStyle.SECONDARY)
            );
        }

        if (buttons.size() == 0) {
            throw new BotWarningException("Cannot create an empty poll");
        }

        int maxPicks = command.hasOption("picks") ? command.get("picks").getAsInt() : 1;
        Arrays.sort(options, String.CASE_INSENSITIVE_ORDER);
        Poll poll = new Poll(message, maxPicks, options);
        polls.put(message.getId(), poll);

        List<ActionRow> rows = new ArrayList<>();
        for (int i = 0; i < buttons.size(); i += 5) {
            List<Button> row = new ArrayList<>();
            for (int j = i; j < Math.min(i + 5, buttons.size()); j++) {
                Button button = buttons.get(j);
                row.add(button);
            }
            if (!row.isEmpty()) {
                rows.add(ActionRow.of(row));
            }
        }

        if (rows.size() > 5) {
            throw new BotWarningException("The amount of options should not exceed 25");
        }

        edit.setComponents(rows);

        if (expiry != null) {
            poll.expiry = LocalDateTime.now().plus(expiry);
            MessageEmbed embed = new EmbedBuilder(edit.getEmbeds().get(0))
                .setTimestamp(poll.expiry.atOffset(OffsetDateTime.now().getOffset()))
                .build();
            edit.setEmbeds(embed);

            executorService.schedule(() -> {
                if (polls.containsKey(poll.id)) {
                    this.closePoll(message, null);
                }
            }, expiry.getSeconds(), TimeUnit.SECONDS);
        }

        getDatabaseManager().setPoll(poll);

        message.editMessage(edit.build()).queue();
    }

    /**
     * Casts a vote for the given user.
     *
     * @param event button event
     * @param reply reply
     */
    private void castVote(ButtonEvent<Member> event, MenuReply reply) {
        String label = event.getButtonId();
        if (!label.startsWith("poll-")) {
            return;
        }
        String[] split = label.split("-", 3);
        String pollId = split[1];
        if (!polls.containsKey(pollId)) {
            throw new BotErrorException("Unknown poll");
        }

        String vote = split[2];
        Poll poll = polls.get(pollId);
        int votesLeft = poll.addVote(event.getActor().getId(), vote);
        getDatabaseManager().setPoll(poll);
        reply.hide();
        reply.ok("Successfully voted for %s, you have %d votes left", vote, votesLeft);
    }

    /**
     * Closes the poll and posts the poll results.
     *
     * @param command command
     * @param reply   reply
     */
    private void getPollResults(CommandEvent command, InteractionReply reply) {
        Message message = URLUtil.messageFromURL(command.get("url").getAsString(), getGuild());
        closePoll(message, reply);
    }

    private void closePoll(Message message, @Nullable InteractionReply reply) {
        if (!message.getAuthor().getId().equals(getGuild().getSelfMember().getId())) {
            throw new BotErrorException("Message was not sent by me");
        }

        if (!polls.containsKey(message.getId())) {
            throw new BotErrorException("Couldn't find poll");
        }

        Poll poll = polls.get(message.getId());
        polls.remove(message.getId());
        getDatabaseManager().removePoll(poll);

        MessageEditBuilder edit = MessageEditBuilder.fromMessage(message)
            .setComponents()
            .setEmbeds(
                message.getEmbeds().get(0),
                new EmbedBuilder()
                    .setTitle("Poll results")
                    .setColor(Colors.TRANSPARENT)
                    .setDescription(poll.toString())
                    .build()
            );
        message.editMessage(edit.build()).queue();

        if (reply != null) {
            reply.hide();
            reply.ok("Poll closed");
        }
    }

    private void showPoll(Message message, InteractionReply reply) {
        if (!message.getAuthor().getId().equals(getGuild().getSelfMember().getId())) {
            throw new BotErrorException("Message was not sent by me");
        }

        if (!polls.containsKey(message.getId())) {
            throw new BotErrorException("Couldn't find poll");
        }

        Poll poll = polls.get(message.getId());

        reply.hide();
        reply.send(new EmbedBuilder()
            .setTitle("Poll results")
            .setColor(Colors.TRANSPARENT)
            .setDescription(poll.toString())
            .build());
    }

    private void getPollInfo(CommandEvent command, InteractionReply reply) {
        Message message = URLUtil.messageFromURL(command.get("url").getAsString(), getGuild());
        showPoll(message, reply);
    }

    public static class Poll {

        private String id;
        private String channelId;
        private final Set<String> choices = new HashSet<>();
        private final Map<String, Set<String>> votes = new HashMap<>();
        private LocalDateTime expiry;
        private int maxPicks;

        private Poll() {}

        public Poll(Message message, int maxPicks, String @NotNull ... choices) {
            this.id = message.getId();
            this.channelId = message.getChannelId();
            this.maxPicks = maxPicks;
            Collections.addAll(this.choices, choices);
        }

        public int addVote(String user, String vote) {
            Set<String> userVotes = votes.get(user);

            if (userVotes == null) {
                userVotes = new HashSet<>();
                userVotes.add(vote);
                votes.put(user, userVotes);
                return maxPicks - userVotes.size();
            }

            if (userVotes.size() >= maxPicks) {
                throw new BotWarningException("You already used %d/%d votes", maxPicks, maxPicks);
            }

            if (!userVotes.add(vote)) {
                throw new BotWarningException("You already voted for `%s`", vote);
            }

            return maxPicks - userVotes.size();
        }

        @JsonIgnore
        public Map<String, Integer> getResults() {
            Map<String, Integer> results = new HashMap<>();
            choices.forEach(choice -> results.put(choice, 0));

            for (Set<String> set : votes.values()) {
                for (String vote : set) {
                    results.computeIfPresent(vote, (v, c) -> c + 1);
                }
            }

            return results;
        }

        public String getId() {
            return id;
        }

        @Override
        public String toString() {
            Map<String, Integer> results = getResults();
            List<String> resultStrings = new ArrayList<>(results.size());

            long total = results.entrySet().stream()
                .sorted(Comparator.comparingInt(e -> -e.getValue()))
                .mapToInt(e -> {
                    resultStrings.add(String.format("- **%s**: `%d`", e.getKey(), e.getValue()));
                    return e.getValue();
                })
                .sum();

            int userCount = votes.size();
            return String.join("\n", resultStrings.toArray(String[]::new))
                + "\nTotal votes: " + total + "\nTotal users: " + userCount;
        }

    }

    @Override
    public String getStatus() {
        return String.format("Enabled: " + isEnabled());
    }

    /**
     * Unescapes the given string.
     * \n -> newline
     * \r -> carriage return
     * \t -> tab
     *
     * @param string the escaped string
     * @return the unescaped string
     */
    @NotNull
    public static String unescape(@NotNull String string) {
        StringBuilder result = new StringBuilder();
        boolean escape = false;
        for (char c : string.toCharArray()) {
            if (escape) {
                result.append(switch (c) {
                    case 'n' -> '\n';
                    case 'r' -> '\r';
                    case 't' -> '\t';
                    default -> c;
                });
                escape = false;
            } else if (c == '\\') {
                escape = true;
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    private static final Pattern timeCheckRegex = Pattern.compile("^(?:\\d+[dhms] ?)+$");
    private static final Pattern timeRegex = Pattern.compile("(\\d+)([dhms]) ?");

    public static Duration formatTime(String input) {
        if (!timeCheckRegex.matcher(input).find()) {
            return null;
        }

        List<Map.Entry<String, String>> list = timeRegex.matcher(input).results()
            .map(result -> Map.entry(result.group(1), result.group(2)))
            .toList();

        Duration duration = Duration.ZERO;

        for (Map.Entry<String, String> entry : list) {
            String numString = entry.getKey();
            String timeUnit = entry.getValue();

            long num = Long.parseLong(numString);

            duration = switch (timeUnit) {
                case "d" -> duration.plusDays(num);
                case "h" -> duration.plusHours(num);
                case "m" -> duration.plusMinutes(num);
                case "s" -> duration.plusSeconds(num);
                default -> null;
            };
            if (duration == null) {
                return null;
            }
        }

        return duration;
    }

}
