package com.thefatrat.eddiejunior.components;

import com.thefatrat.eddiejunior.entities.Command;
import com.thefatrat.eddiejunior.exceptions.BotErrorException;
import com.thefatrat.eddiejunior.exceptions.BotWarningException;
import com.thefatrat.eddiejunior.sources.Server;
import com.thefatrat.eddiejunior.util.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class PollComponent extends Component {

    private final Map<String, Poll> polls = new HashMap<>();

    public PollComponent(Server server) {
        super(server, "Poll", false);

        setComponentCommand();

        addSubcommands(
            new Command("new", "create a new poll")
                .addOptions(
                    new OptionData(OptionType.STRING, "text", "text", true)
                        .setMaxLength(1000),
                    new OptionData(OptionType.STRING, "option1", "option1", true),
                    new OptionData(OptionType.STRING, "option2", "option2", true),
                    new OptionData(OptionType.STRING, "option3", "option3", false),
                    new OptionData(OptionType.STRING, "option4", "option4", false),
                    new OptionData(OptionType.STRING, "option5", "option5", false),
                    new OptionData(OptionType.STRING, "option6", "option6", false),
                    new OptionData(OptionType.STRING, "option7", "option7", false),
                    new OptionData(OptionType.STRING, "option8", "option8", false),
                    new OptionData(OptionType.STRING, "option9", "option9", false),
                    new OptionData(OptionType.STRING, "option10", "option10", false),
                    new OptionData(OptionType.CHANNEL, "channel", "channel", false)
                        .setChannelTypes(ChannelType.TEXT)
                )
                .setAction((command, reply) -> {
                    MessageCreateBuilder builder = new MessageCreateBuilder();

                    Optional.ofNullable(command.getArgs().get("text"))
                        .ifPresent(text ->
                            builder.addEmbeds(
                                new EmbedBuilder()
                                    .setDescription(text.getAsString())
                                    .setColor(Colors.TRANSPARENT)
                                    .build()
                            )
                        );

                    Consumer<Message> consumer = message -> {
                        MessageEditBuilder edit = MessageEditBuilder.fromMessage(message);
                        List<Button> buttons = new ArrayList<>();
                        List<String> options = new ArrayList<>();
                        Set<String> ids = new HashSet<>();
                        int added = 0;

                        for (int i = 1; i <= 10; i++) {
                            OptionMapping option = command.getArgs().get("option" + i);
                            if (option == null) {
                                continue;
                            }
                            Button temp = EmojiUtil.formatButton("p", option.getAsString(), ButtonStyle.SECONDARY);
                            String label;
                            if ("".equals(temp.getLabel())) {
                                label = Objects.requireNonNull(temp.getEmoji()).asUnicode().getAsCodepoints();
                            } else {
                                label = temp.getLabel();
                            }
                            String id = message.getId() + "-" + label;
                            ids.add(id);
                            added++;
                            if (ids.size() != added) {
                                message.editMessageEmbeds(
                                        new EmbedBuilder()
                                            .setColor(Icon.ERROR.getColor())
                                            .setDescription(Icon.ERROR + " All poll options should be unique")
                                            .build()
                                    )
                                    .queue();
                                return;
                            }
                            buttons.add(
                                EmojiUtil.formatButton("poll-" + id, option.getAsString(), ButtonStyle.SECONDARY)
                            );
                            options.add(temp.getLabel());
                        }

                        polls.put(message.getId(), new Poll(options.toArray(String[]::new)));

                        edit.setActionRow(buttons);
                        message.editMessage(edit.build()).queue();
                    };

                    Optional.ofNullable(command.getArgs().get("channel"))
                        .ifPresentOrElse(
                            object -> {
                                TextChannel channel = object.getAsChannel().asTextChannel();
                                PermissionChecker.requireSend(channel);
                                channel.sendMessage(builder.build()).queue();
                                reply.ok(consumer, "Message sent in %s", channel.getAsMention());
                            },
                            () -> reply.send(builder.build(), consumer)
                        );
                }),

            new Command("results", "close poll and show results")
                .addOption(new OptionData(OptionType.STRING, "url", "message url", true))
                .setAction((command, reply) -> {
                    Message message = URLUtil.messageFromURL(
                        command.getArgs().get("url").getAsString(), getServer().getGuild());

                    if (!message.getAuthor().getId().equals(getServer().getGuild().getSelfMember().getId())) {
                        throw new BotErrorException("Message was not sent by me");
                    }

                    if (!polls.containsKey(message.getId())) {
                        throw new BotErrorException("Couldn't find poll");
                    }

                    PermissionChecker.requireSend(message.getGuildChannel().getPermissionContainer());

                    MessageEditBuilder edit = MessageEditBuilder.fromMessage(message);
                    edit.setComponents();
                    message.editMessage(edit.build()).queue();

                    Map<String, Integer> results = polls.get(message.getId()).getResults();
                    polls.remove(message.getId());

                    List<String> resultStrings = new ArrayList<>(results.size());
                    AtomicInteger total = new AtomicInteger();
                    results.forEach((vote, count) -> {
                        resultStrings.add("**" + vote + "**: " + count);
                        total.addAndGet(count);
                    });

                    String joined = String.join("\n", resultStrings.toArray(String[]::new));

                    MessageEmbed embed = new EmbedBuilder()
                        .setTitle("Poll results")
                        .setColor(Colors.TRANSPARENT)
                        .setDescription(joined)
                        .build();

                    reply.send(embed);
                })
        );

        getServer().getButtonHandler().addListener((event, reply) -> {
            String label = event.getButtonId();
            if (!label.startsWith("poll-")) {
                return;
            }
            String[] split = label.split("-", 3);
            String pollId = split[1];
            if (!polls.containsKey(pollId)) {
                throw new BotErrorException("Vote could not be made");
            }

            String vote = split[2];
            if (polls.get(pollId).addVote(vote, event.getUser().getId())) {
                reply.hide();
                reply.ok("Successfully voted for `%s`", vote);
            } else {
                throw new BotWarningException("Could not vote on this poll");
            }
        });

    }

    private static class Poll {

        private final Map<String, Integer> results = new HashMap<>();
        private final Set<String> votes = new HashSet<>();

        public Poll(String... choices) {
            for (String choice : choices) {
                results.put(choice, 0);
            }
        }

        public boolean addVote(String vote, String user) {
            if (votes.contains(user)) {
                return false;
            }
            results.computeIfPresent(vote, (k, v) -> v + 1);
            votes.add(user);
            return true;
        }

        public Map<String, Integer> getResults() {
            return results;
        }

    }

}
