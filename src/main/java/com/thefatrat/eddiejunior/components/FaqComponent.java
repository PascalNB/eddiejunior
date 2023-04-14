package com.thefatrat.eddiejunior.components;

import com.thefatrat.eddiejunior.Bot;
import com.thefatrat.eddiejunior.entities.Command;
import com.thefatrat.eddiejunior.exceptions.BotErrorException;
import com.thefatrat.eddiejunior.exceptions.BotWarningException;
import com.thefatrat.eddiejunior.sources.Server;
import com.thefatrat.eddiejunior.util.Colors;
import com.thefatrat.eddiejunior.util.EmojiUtil;
import com.thefatrat.eddiejunior.util.Icon;
import com.thefatrat.eddiejunior.util.PermissionChecker;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.internal.requests.CompletedRestAction;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;
import java.util.stream.IntStream;

public class FaqComponent extends Component {

    private final List<String> faqList = new ArrayList<>();
    private final Map<String, String[]> faqMap = new HashMap<>();
    private Message storageMessage;
    private Message faqMessage;

    public FaqComponent(Server server) {
        super(server, "Faq", false);

        {
            String storageMessageId = getDatabaseManager().getSetting("storagemessage");
            if (storageMessageId != null) {
                String[] split = storageMessageId.split("_", 2);
                TextChannel channel = getServer().getGuild().getTextChannelById(split[0]);
                if (channel != null) {
                    try {
                        storageMessage = channel.retrieveMessageById(split[1]).onErrorMap(e -> null).complete();
                        String data = storageMessage.getEmbeds().get(0).getDescription();
                        parseJSON(data, faqList, faqMap);
                    } catch (InsufficientPermissionException ignore) {
                    }
                }
            }
            String faqMessageId = getDatabaseManager().getSetting("faqmessage");
            if (faqMessageId != null) {
                String[] split = faqMessageId.split("_", 2);
                TextChannel channel = getServer().getGuild().getTextChannelById(split[0]);
                if (channel != null) {
                    try {
                        faqMessage = channel.retrieveMessageById(split[1]).onErrorMap(e -> null).complete();
                    } catch (InsufficientPermissionException ignore) {
                    }
                }
            }
        }

        setComponentCommand();

        addSubcommands(
            new Command("storage", "create a new storage message where the faq data will be kept")
                .addOption(
                    new OptionData(OptionType.CHANNEL, "channel", "channel", true)
                        .setChannelTypes(ChannelType.TEXT)
                )
                .setAction((command, reply) -> {
                    TextChannel channel = command.getArgs().get("channel").getAsChannel().asTextChannel();
                    PermissionChecker.requireSend(channel);

                    storageMessage = channel.sendMessageEmbeds(
                        new EmbedBuilder().setDescription("tmp").build()
                    ).complete();
                    reply.ok("Storage message sent\n%s", storageMessage.getJumpUrl());
                    updateStorage(storageMessage).queue();
                    getDatabaseManager().setSetting("storagemessage",
                        channel.getId() + "_" + storageMessage.getId());
                    getServer().log(command.getMember().getUser(), "Created a new storage message in %s (`%s`)\n%s",
                        channel.getAsMention(), channel.getId(), storageMessage.getJumpUrl());
                }),

            new Command("add", "add a new question and answer")
                .setAction((command, reply) ->
                    reply.sendModal(Modal.create("faq_add", "Add question")
                        .addActionRow(
                            TextInput.create("question", "Question", TextInputStyle.PARAGRAPH)
                                .setRequiredRange(10, 150)
                                .build()
                        )
                        .addActionRow(
                            TextInput.create("answer", "Answer", TextInputStyle.PARAGRAPH)
                                .setRequiredRange(1, 300)
                                .build()
                        )
                        .addActionRow(TextInput.create("emoji", "Emoji", TextInputStyle.SHORT)
                            .setRequired(false)
                            .build()
                        )
                        .build())
                ),

            new Command("remove", "remove a question from the faq list")
                .setAction((command, reply) -> {
                    if (faqList.isEmpty()) {
                        throw new BotWarningException("The list of questions is empty");
                    }

                    reply.hide();
                    reply.send(new MessageCreateBuilder()
                        .addEmbeds(new EmbedBuilder()
                            .setColor(Colors.TRANSPARENT)
                            .setDescription("Select a question that should be removed")
                            .build())
                        .addActionRow(StringSelectMenu.create("faq_remove")
                            .addOptions(getOptions(faqList))
                            .build())
                        .build());
                }),

            new Command("edit", "edit the answer of a question")
                .setAction((command, reply) -> {
                    if (faqList.isEmpty()) {
                        throw new BotWarningException("The list of questions is empty");
                    }

                    reply.hide();
                    reply.send(new MessageCreateBuilder()
                        .addEmbeds(new EmbedBuilder()
                            .setColor(Colors.TRANSPARENT)
                            .setDescription("Select the question that needs to be edited")
                            .build())
                        .addActionRow(StringSelectMenu.create("faq_edit")
                            .addOptions(getOptions(faqList))
                            .build())
                        .build());
                }),

            new Command("message", "send the faq message")
                .addOption(new OptionData(OptionType.STRING, "text", "text", false).setMinLength(1))
                .setAction((command, reply) -> {
                    if (faqList.isEmpty()) {
                        throw new BotWarningException("The list of questions is empty");
                    }

                    String text = Optional.ofNullable(command.getArgs().get("text"))
                        .map(OptionMapping::getAsString)
                        .orElse("Select a question");

                    if (text.isBlank()) {
                        throw new BotWarningException("Text cannot be empty");
                    }

                    command.getChannel().sendMessage(
                            new MessageCreateBuilder()
                                .addEmbeds(new EmbedBuilder()
                                    .setColor(Colors.TRANSPARENT)
                                    .setTitle("Frequently Asked Questions")
                                    .setDescription(text)
                                    .build())
                                .addActionRow(StringSelectMenu.create("faq_query")
                                    .addOptions(getOptions(faqList))
                                    .setPlaceholder("Select a question")
                                    .setMaxValues(1)
                                    .build())
                                .build())
                        .queue(m -> {
                            faqMessage = m;
                            getDatabaseManager().setSetting("faqmessage", m.getChannel().getId() + "_" + m.getId());
                        });

                    reply.hide();
                    reply.ok("Faq message sent");
                })
        );

        getServer().getModalHandler().addListener("faq_add", (event, reply) -> {
            if (faqList.size() == 25) {
                throw new BotWarningException("There can be 25 questions added at most");
            }

            Map<String, ModalMapping> map = event.getValues();
            String question = map.get("question").getAsString();
            String answer = map.get("answer").getAsString();
            String emoji = map.get("emoji").getAsString();
            String questionLowercase = question.toLowerCase();

            if (question.isBlank() || answer.isBlank()) {
                throw new BotWarningException("Blank fields are not allowed");
            }

            if (faqMap.containsKey(questionLowercase)) {
                throw new BotWarningException("That question has already been added");
            }

            if (emoji.isEmpty()) {
                emoji = null;
            } else if (!EmojiUtil.isEmoji(emoji)) {
                throw new BotErrorException("%s is not a valid emoji", emoji);
            }

            faqList.add(question);
            faqMap.put(questionLowercase, new String[]{answer, emoji});

            reply.hide();
            reply.ok("Added question and answer for `%s`", question);
            getServer().log(event.getMember().getUser(), "Added question and answer for `%s`", question);
            updateMessage(faqMessage).queue();
            updateStorage(storageMessage).queue();
        });

        getServer().getStringSelectHandler().addListener("faq_remove", (event, reply) -> {
            int index = Integer.parseInt(event.getOption().getValue());
            String question = event.getOption().getLabel();

            if (index >= faqList.size() || !faqList.get(index).equals(question)) {
                throw new BotErrorException("Something went wrong, try again");
            }

            faqList.remove(index);
            faqMap.remove(question.toLowerCase());

            reply.edit(Icon.STOP, "Removed question `%s`", question);
            getServer().log(event.getUser(), "Removed question `%s`", question);
            updateMessage(faqMessage).queue();
            updateStorage(storageMessage).queue();
        });

        getServer().getStringSelectHandler().addListener("faq_edit", (event, reply) -> {
            int index = Integer.parseInt(event.getOption().getValue());
            String question = event.getOption().getLabel();

            if (index >= faqList.size() || !faqList.get(index).equals(question)) {
                throw new BotErrorException("Something went wrong, try again");
            }

            int hash = question.hashCode();

            String[] answer = faqMap.get(question.toLowerCase());

            reply.sendModal(Modal.create("faq_edit", "Edit answer")
                .addActionRow(
                    TextInput.create("q-" + index + "-" + hash, "Question", TextInputStyle.PARAGRAPH)
                        .setRequiredRange(10, 150)
                        .setValue(question)
                        .build()
                )
                .addActionRow(
                    TextInput.create("a-" + index + "-" + hash, "Answer", TextInputStyle.PARAGRAPH)
                        .setRequiredRange(1, 350)
                        .setValue(answer[0])
                        .build()
                )
                .addActionRow(
                    TextInput.create("e-" + index + "-" + hash, "Emoji", TextInputStyle.SHORT)
                        .setRequired(false)
                        .setValue(answer[1])
                        .build()
                )
                .build());
        });

        getServer().getModalHandler().addListener("faq_edit", (event, reply) -> {
            Map<String, ModalMapping> values = event.getValues();
            String key = values.keySet().iterator().next();
            String[] split = key.split("-", 3);

            int index = Integer.parseInt(split[1]);
            int hash = Integer.parseInt(split[2]);
            String newQuestion = values.get("q-" + index + "-" + hash).getAsString();
            String newAnswer = values.get("a-" + index + "-" + hash).getAsString();
            String emoji = values.get("e-" + index + "-" + hash).getAsString();

            if (index >= faqList.size()) {
                throw new BotErrorException("Something went wrong, try again");
            }
            if (newQuestion.isBlank() || newAnswer.isBlank()) {
                throw new BotWarningException("Answer cannot be blank");
            }
            String newEmoji;
            if (emoji.isEmpty()) {
                newEmoji = null;
            } else if (EmojiUtil.isEmoji(emoji)) {
                newEmoji = emoji;
            } else {
                throw new BotErrorException("%s is not a valid emoji", emoji);
            }

            String oldQuestion = faqList.get(index);
            String oldKey = oldQuestion.toLowerCase();
            String oldEmoji = faqMap.get(oldKey)[1];

            if (oldQuestion.hashCode() != hash) {
                throw new BotErrorException("Something went wrong, try again");
            }

            faqList.set(index, newQuestion);
            String newKey = newQuestion.toLowerCase();

            if (faqMap.containsKey(newKey)) {
                faqMap.computeIfPresent(newKey, (k, v) -> {
                    v[0] = newAnswer;
                    v[1] = newEmoji;
                    return v;
                });
            } else {
                String[] oldData = faqMap.remove(oldKey);
                oldData[0] = newAnswer;
                oldData[1] = newEmoji;
                faqMap.put(newKey, oldData);
            }

            reply.hide();
            if (newQuestion.equals(oldQuestion)) {
                reply.ok("Edited question `%s`", oldQuestion);
                getServer().log(event.getMember().getUser(), "Edited question `%s`", oldQuestion);
                if (!Objects.equals(oldEmoji, newEmoji)) {
                    updateMessage(faqMessage).queue();
                }
            } else {
                reply.ok("Edited question `%s` -> `%s`", oldQuestion, newQuestion);
                getServer().log(event.getMember().getUser(), "Edited question `%s` -> `%s`", oldQuestion, newQuestion);
                updateMessage(faqMessage).queue();
            }
            updateStorage(storageMessage).queue();
        });

        getServer().getStringSelectHandler().addListener("faq_query", (event, reply) -> {
            String question = event.getOption().getLabel();
            String answer = faqMap.get(question.toLowerCase())[0];
            if (answer == null) {
                throw new BotErrorException("Something went wrong, contact the moderators");
            }

            reply.hide();
            reply.send(new EmbedBuilder()
                .setColor(Colors.TRANSPARENT)
                .setTitle(question)
                .setDescription(answer)
                .build());
        });
    }

    private SelectOption[] getOptions(List<String> list) {
        return IntStream.range(0, list.size())
            .mapToObj(i -> {
                String q = list.get(i);
                SelectOption option = SelectOption.of(q, String.valueOf(i));
                String emoji = faqMap.get(q.toLowerCase())[1];
                if (emoji != null) {
                    option = option.withEmoji(Emoji.fromUnicode(emoji));
                }
                return option;
            })
            .sorted(Comparator.comparing(SelectOption::getLabel))
            .toArray(SelectOption[]::new);
    }

    private RestAction<Message> updateStorage(Message message) {
        if (message == null || !message.getChannel().canTalk()) {
            return new CompletedRestAction<>(Bot.getInstance().getJDA(), null);
        }

        JSONArray array = new JSONArray();
        for (String question : faqList) {
            String[] answer = faqMap.get(question.toLowerCase());
            String emoji = answer[1];

            JSONObject object = new JSONObject()
                .put("q", question)
                .put("a", answer[0]);

            if (emoji != null) {
                object.put("e", emoji);
            }

            array.put(object);
        }
        try {
            return message.editMessageEmbeds(new EmbedBuilder()
                    .setColor(Colors.TRANSPARENT)
                    .setDescription(array.toString())
                    .build())
                .onErrorMap(e -> null);
        } catch (IllegalArgumentException e) {
            throw new BotErrorException("Max faq limit reached");
        }
    }

    private RestAction<Message> updateMessage(Message message) {
        if (message == null || !message.getChannel().canTalk()) {
            return new CompletedRestAction<>(Bot.getInstance().getJDA(), null);
        }

        if (!faqList.isEmpty()) {
            return message.editMessageComponents(
                    ActionRow.of(StringSelectMenu.create("faq_query")
                        .addOptions(getOptions(faqList))
                        .setPlaceholder("Select a question")
                        .setMaxValues(1)
                        .build()
                    )
                )
                .onErrorMap(e -> null);
        }
        return message.editMessageComponents().onErrorMap(e -> null);
    }

    private void parseJSON(String data, List<String> list, Map<String, String[]> map) {
        if (data != null) {
            JSONArray array = new JSONArray(data);
            for (int i = 0; i < array.length(); i++) {
                JSONObject element = array.getJSONObject(i);
                String q = element.getString("q");
                String a = element.getString("a");
                String e = element.optString("e", null);
                list.add(q);
                map.put(q.toLowerCase(), new String[]{a, e});
            }
        }
    }

    @Override
    public String getStatus() {
        return String.format("""
                Enabled: %b
                Questions: %d
                Message: %s
                Storage: %s
                """,
            isEnabled(), faqList.size(),
            Optional.ofNullable(faqMessage).map(Message::getJumpUrl).orElse("null"),
            Optional.ofNullable(storageMessage).map(Message::getJumpUrl).orElse("null"));
    }

}
