package com.thefatrat.eddiejunior.components.impl;

import com.thefatrat.eddiejunior.components.AbstractComponent;
import com.thefatrat.eddiejunior.entities.Command;
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
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;

public class PollComponent extends AbstractComponent {

    private final Map<String, Poll> polls = new HashMap<>();

    public PollComponent(Server server) {
        super(server, "Poll");

        setComponentCommand();

        addSubcommands(
            new Command("new", "create a new poll")
                .addOptions(
                    new OptionData(OptionType.STRING, "text", "text", true)
                        .setMaxLength(1000),
                    new OptionData(OptionType.STRING, "options", "options seperated by commas", true),
                    new OptionData(OptionType.INTEGER, "picks", "maximum number of options a user can choose", false)
                        .setMinValue(1)
                        .setMaxValue(100),
                    new OptionData(OptionType.CHANNEL, "channel", "channel that the poll will be sent in", false)
                        .setChannelTypes(ChannelType.TEXT)
                )
                .setAction(this::newPoll),

            new Command("results", "close poll and show results")
                .addOptions(new OptionData(OptionType.STRING, "url", "message url", true))
                .setAction(this::getPollResults)
        );

        getServer().getButtonHandler().addListener(this::castVote);
    }

    /**
     * Sends a new message with a new poll with the given options.
     *
     * @param command command
     * @param reply   reply
     */
    private void newPoll(CommandEvent command, InteractionReply reply) {
        MessageCreateBuilder builder = new MessageCreateBuilder();
        String text = command.get("text").getAsString();

        builder.addEmbeds(new EmbedBuilder()
            .setDescription(text)
            .setColor(Colors.TRANSPARENT)
            .build()
        );

        Consumer<Message> consumer = message -> {
            try {
                this.createPoll(message, command);
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
    private void createPoll(Message message, CommandEvent command) throws BotException {
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
                label = Objects.requireNonNull(temp.getEmoji()).asUnicode().getAsCodepoints();
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
        polls.put(message.getId(), new Poll(maxPicks, options));

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
        reply.hide();
        reply.ok("Successfully voted for `%s`, you have %d votes left", vote, votesLeft);
    }

    /**
     * Closes the poll and posts the poll results.
     *
     * @param command command
     * @param reply   reply
     */
    private void getPollResults(CommandEvent command, InteractionReply reply) {
        Message message = URLUtil.messageFromURL(command.get("url").getAsString(), getGuild());

        if (!message.getAuthor().getId().equals(getGuild().getSelfMember().getId())) {
            throw new BotErrorException("Message was not sent by me");
        }

        if (!polls.containsKey(message.getId())) {
            throw new BotErrorException("Couldn't find poll");
        }

        PermissionChecker.requireSend(message.getGuildChannel().getPermissionContainer());

        MessageEditBuilder edit = MessageEditBuilder.fromMessage(message);
        edit.setComponents();
        message.editMessage(edit.build()).queue();

        Poll poll = polls.get(message.getId());
        Map<String, Integer> results = poll.getResults();
        polls.remove(message.getId());

        List<String> resultStrings = new ArrayList<>(results.size());
        long total = results.entrySet().stream()
            .sorted(Comparator.comparingInt(e -> -e.getValue()))
            .mapToInt(e -> {
                resultStrings.add(String.format("- **%s**: `%d`", e.getKey(), e.getValue()));
                return e.getValue();
            })
            .sum();
        int userCount = poll.votes.size();

        String joined = String.join("\n", resultStrings.toArray(String[]::new))
            + "\nTotal votes: " + total + "\nTotal users: " + userCount;

        MessageEmbed embed = new EmbedBuilder()
            .setTitle("Poll results")
            .setColor(Colors.TRANSPARENT)
            .setDescription(joined)
            .build();

        reply.send(embed);
    }

    private static class Poll {

        private final Set<String> choices = new HashSet<>();
        private final Map<String, Set<String>> votes = new HashMap<>();
        private final int maxPicks;

        public Poll(int maxPicks, String @NotNull ... choices) {
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

    }

    @Override
    public String getStatus() {
        return String.format("Enabled: " + isEnabled());
    }

}
