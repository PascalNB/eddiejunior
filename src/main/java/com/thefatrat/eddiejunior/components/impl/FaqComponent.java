package com.thefatrat.eddiejunior.components.impl;

import com.thefatrat.eddiejunior.Bot;
import com.thefatrat.eddiejunior.components.AbstractComponent;
import com.thefatrat.eddiejunior.entities.Command;
import com.thefatrat.eddiejunior.events.CommandEvent;
import com.thefatrat.eddiejunior.events.ModalEvent;
import com.thefatrat.eddiejunior.events.SelectEvent;
import com.thefatrat.eddiejunior.exceptions.BotErrorException;
import com.thefatrat.eddiejunior.exceptions.BotWarningException;
import com.thefatrat.eddiejunior.reply.DefaultReply;
import com.thefatrat.eddiejunior.reply.InteractionReply;
import com.thefatrat.eddiejunior.reply.MenuReply;
import com.thefatrat.eddiejunior.sources.Server;
import com.thefatrat.eddiejunior.util.Colors;
import com.thefatrat.eddiejunior.util.EmojiUtil;
import com.thefatrat.eddiejunior.util.Icon;
import com.thefatrat.eddiejunior.util.URLUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
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
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.util.*;

public class FaqComponent extends AbstractComponent {

    private final Map<Integer, Question> questions = new HashMap<>();
    private int questionId = -1;
    private Message faqMessage;

    public FaqComponent(Server server) {
        super(server, "Faq");

        {
            List<Question> questions = getDatabaseManager().getQuestions();
            for (Question question : questions) {
                this.questions.put(question.id(), question);
                this.questionId = Math.max(questionId, question.id());
            }

            String faqMessageId = getDatabaseManager().getSetting("faqmessage");
            if (faqMessageId != null) {
                String[] split = faqMessageId.split("_", 2);
                TextChannel channel = getGuild().getTextChannelById(split[0]);
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
            new Command("add", "add a new question and answer")
                .setAction(this::addQuestion),

            new Command("remove", "remove a question from the faq list")
                .setAction(this::removeQuestion),

            new Command("edit", "edit the answer of a question")
                .setAction((c, r) -> this.editQuestion(r)),

            new Command("message", "send the faq message")
                .addOptions(new OptionData(OptionType.STRING, "text", "text", false).setMinLength(1))
                .setAction(this::sendFaqMesasge),

            new Command("answer", "sends the answer for one of the faq questions")
                .addOptions(new OptionData(OptionType.USER, "user", "user ping", false))
                .setAction(this::answerQuestion)
        );

        getServer().getModalHandler().addListener("faq_add", this::handleAddModal);

        getServer().getStringSelectHandler().addListener("faq_remove", this::handleRemoveSelectMenu);

        getServer().getStringSelectHandler().addListener("faq_edit", this::handleEditSelectMenu);

        getServer().getModalHandler().addListener("faq_edit", this::handleEditModal);

        getServer().getStringSelectHandler().addListener("faq_query", this::handleFaqQuery);

        getServer().getStringSelectHandler().addListener("faq_answer", this::handleFaqAnswer);
    }

    private void addQuestion(CommandEvent command, InteractionReply reply) {
        reply.sendModal(Modal.create("faq_add", "Add question")
            .addActionRow(TextInput.create("question", "Question", TextInputStyle.SHORT)
                .setRequiredRange(10, 100)
                .build()
            )
            .addActionRow(TextInput.create("description", "Description", TextInputStyle.SHORT)
                .setRequiredRange(0, 100)
                .setRequired(false)
                .build()
            )
            .addActionRow(TextInput.create("answer", "Answer", TextInputStyle.PARAGRAPH)
                .setRequiredRange(1, 300)
                .build()
            )
            .addActionRow(TextInput.create("emoji", "Emoji", TextInputStyle.SHORT)
                .setRequired(false)
                .build()
            )
            .addActionRow(TextInput.create("url", "Image URL", TextInputStyle.SHORT)
                .setRequired(false)
                .build()
            )
            .build());
    }

    private void removeQuestion(CommandEvent command, InteractionReply reply) {
        if (this.questions.isEmpty()) {
            throw new BotWarningException("The list of questions is empty");
        }

        reply.hide();
        reply.send(new MessageCreateBuilder()
            .addEmbeds(new EmbedBuilder()
                .setColor(Colors.TRANSPARENT)
                .setDescription("Select a question that should be removed")
                .build())
            .addActionRow(StringSelectMenu.create("faq_remove")
                .addOptions(getOptions())
                .build())
            .build());
    }

    private void editQuestion(InteractionReply reply) {
        if (this.questions.isEmpty()) {
            throw new BotWarningException("The list of questions is empty");
        }

        reply.hide();
        reply.send(new MessageCreateBuilder()
            .addEmbeds(new EmbedBuilder()
                .setColor(Colors.TRANSPARENT)
                .setDescription("Select the question that needs to be edited")
                .build())
            .addActionRow(StringSelectMenu.create("faq_edit")
                .addOptions(getOptions())
                .build())
            .build());
    }

    private void answerQuestion(CommandEvent command, InteractionReply reply) {
        if (this.questions.isEmpty()) {
            throw new BotWarningException("The list of questions is empty");
        }

        MessageCreateBuilder builder = new MessageCreateBuilder()
            .addEmbeds(new EmbedBuilder()
                .setColor(Colors.TRANSPARENT)
                .setDescription("Select the question that should be answered")
                .build()
            )
            .addActionRow(StringSelectMenu.create("faq_answer")
                .addOptions(getOptions())
                .build()
            );

        if (command.hasOption("user")) {
            User user = command.get("user").getAsUser();
            builder.setContent(user.getAsMention());
        }

        reply.hide();
        reply.send(builder.build());
    }

    private void sendFaqMesasge(CommandEvent command, InteractionReply reply) {
        if (this.questions.isEmpty()) {
            throw new BotWarningException("The list of questions is empty");
        }

        String text = Optional.ofNullable(command.get("text"))
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
                        .addOptions(getOptions())
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
    }

    private void handleAddModal(ModalEvent event, DefaultReply reply) {
        Map<String, ModalMapping> map = event.getValues();
        String question = map.get("question").getAsString();
        String answer = map.get("answer").getAsString();
        String emoji = map.get("emoji").getAsString();
        String desc = map.get("description").getAsString();
        String url = map.get("url").getAsString();

        if (question.isBlank() || answer.isBlank()) {
            throw new BotWarningException("Blank fields are not allowed");
        }

        if (url.isBlank()) {
            url = null;
        } else if (URLUtil.matchUrl(url) == null) {
            throw new BotWarningException("Please specify a correct URL");
        }

        if (emoji.isEmpty()) {
            emoji = null;
        } else if (!EmojiUtil.isEmoji(emoji)) {
            throw new BotErrorException("%s is not a valid emoji", emoji);
        }
        if (desc.isBlank()) {
            desc = null;
        }

        questionId++;
        Question faqQuestion = new Question(questionId, question, answer, desc, emoji, url);
        this.questions.put(questionId, faqQuestion);

        reply.hide();
        reply.ok("Added question and answer for `%s`", question);
        getServer().log(event.getMember().getUser(), "Added question and answer for `%s`", question);
        updateMessage(faqMessage).queue();
        getDatabaseManager().setQuestion(questionId, faqQuestion.toJson());
    }

    private void handleRemoveSelectMenu(SelectEvent<SelectOption> event, MenuReply reply) {
        int id = Integer.parseInt(event.getOption().getValue());
        String question = event.getOption().getLabel();

        if (!question.equals(this.questions.get(id).question())) {
            throw new BotErrorException("Something went wrong, try again");
        }

        this.questions.remove(id);

        reply.edit(Icon.STOP, "Removed question `%s`", question);
        getServer().log(event.getUser(), "Removed question `%s`", question);
        updateMessage(faqMessage).queue();
        getDatabaseManager().removeQuestion(id);
    }

    private void handleEditSelectMenu(SelectEvent<SelectOption> event, MenuReply reply) {
        int id = Integer.parseInt(event.getOption().getValue());
        String question = event.getOption().getLabel();

        Question faqQuestion = this.questions.get(id);

        if (!question.equals(faqQuestion.question())) {
            throw new BotErrorException("Something went wrong, try again");
        }

        reply.sendModal(Modal.create("faq_edit", "Edit answer")
            .addActionRow(TextInput.create("q-" + id, "Question", TextInputStyle.SHORT)
                .setRequiredRange(10, 150)
                .setValue(question)
                .build()
            )
            .addActionRow(TextInput.create("d-" + id, "Description", TextInputStyle.SHORT)
                .setRequiredRange(0, 100)
                .setRequired(false)
                .setValue(faqQuestion.description())
                .build()
            )
            .addActionRow(TextInput.create("a-" + id, "Answer", TextInputStyle.PARAGRAPH)
                .setRequiredRange(1, 350)
                .setValue(faqQuestion.answer())
                .build()
            )
            .addActionRow(TextInput.create("e-" + id, "Emoji", TextInputStyle.SHORT)
                .setRequired(false)
                .setValue(faqQuestion.emoji())
                .build()
            )
            .addActionRow(TextInput.create("u-" + id, "Image URL", TextInputStyle.SHORT)
                .setRequired(false)
                .setValue(faqQuestion.url())
                .build()
            )
            .build());
    }

    private void handleEditModal(ModalEvent event, DefaultReply reply) {
        Map<String, ModalMapping> values = event.getValues();
        String key = values.keySet().iterator().next();
        String[] split = key.split("-", 2);

        int id = Integer.parseInt(split[1]);
        String append = "-" + id;
        String newQuestion = values.get('q' + append).getAsString();
        String newAnswer = values.get('a' + append).getAsString();
        String newEmoji = values.get('e' + append).getAsString();
        String newDesc = values.get('d' + append).getAsString();
        String newUrl = values.get('u' + append).getAsString();

        if (newQuestion.isBlank() || newAnswer.isBlank()) {
            throw new BotWarningException("Answer cannot be blank");
        }

        if (newUrl.isBlank()) {
            newUrl = null;
        } else if (URLUtil.matchUrl(newUrl) == null) {
            throw new BotWarningException("Please specify a correct URL");
        }

        if (newDesc.isBlank()) {
            newDesc = null;
        }

        if (newEmoji.isEmpty()) {
            newEmoji = null;
        } else if (!EmojiUtil.isEmoji(newEmoji)) {
            throw new BotErrorException("%s is not a valid emoji", newEmoji);
        }

        Question oldFaqQuestion = this.questions.get(id);

        String oldQuestion = oldFaqQuestion.question();
        String oldAnswer = oldFaqQuestion.answer();
        String oldEmoji = oldFaqQuestion.emoji();
        String oldDesc = oldFaqQuestion.description();
        String oldUrl = oldFaqQuestion.url();

        boolean changeQ = !newQuestion.equals(oldQuestion);
        boolean changeA = !oldAnswer.equals(newAnswer);
        boolean changeE = !Objects.equals(oldEmoji, newEmoji);
        boolean changeD = !Objects.equals(oldDesc, newDesc);
        boolean changeU = !Objects.equals(oldUrl, newUrl);

        if (!(changeQ || changeA || changeE || changeD || changeU)) {
            throw new BotWarningException("No changes have been made");
        }

        Question newFaqQuestion = new Question(id, newQuestion, newAnswer, newDesc, newEmoji, newUrl);
        this.questions.put(id, newFaqQuestion);

        reply.hide();
        reply.ok("Edited question `%s`", oldQuestion);

        List<String> changes = new ArrayList<>();
        if (changeQ) {
            changes.add(String.format("- `%s` → `%s`", oldQuestion, newQuestion));
        }
        if (changeD) {
            changes.add(String.format("- `%s` → `%s`", oldDesc, newDesc));
        }
        if (changeE) {
            changes.add(String.format("- %s → %s", oldEmoji, newEmoji));
        }
        if (changeU) {
            changes.add(String.format("- `%s` → `%s`", oldUrl, newUrl));
        }
        if (changeA) {
            changes.add("- Answer modified");
        }
        String changeString = String.join("\n", changes);

        getServer().log(event.getMember().getUser(), "Edited question `%s`:\n%s", oldQuestion, changeString);
        if (changeQ || changeE || changeD) {
            updateMessage(faqMessage).queue();
        }
        getDatabaseManager().setQuestion(id, newFaqQuestion.toJson());
    }

    private void handleFaqQuery(SelectEvent<SelectOption> event, MenuReply reply) {
        int id = Integer.parseInt(event.getOption().getValue());
        Question faqQuestion = this.questions.get(id);
        if (faqQuestion == null) {
            throw new BotErrorException("Something went wrong, contact the moderators");
        }

        reply.hide();
        reply.send(new EmbedBuilder()
            .setColor(Colors.TRANSPARENT)
            .setTitle(faqQuestion.question())
            .setDescription(faqQuestion.answer())
            .setImage(faqQuestion.url())
            .build());
    }

    private void handleFaqAnswer(SelectEvent<SelectOption> event, MenuReply reply) {
        int id = Integer.parseInt(event.getOption().getValue());
        Question faqQuestion = this.questions.get(id);
        if (faqQuestion == null) {
            throw new BotErrorException("Something went wrong");
        }

        MessageCreateBuilder builder = new MessageCreateBuilder()
            .setContent(event.getMessage().getContentRaw())
            .addEmbeds(new EmbedBuilder()
                .setColor(Colors.TRANSPARENT)
                .setTitle(faqQuestion.question())
                .setDescription(faqQuestion.answer())
                .setImage(faqQuestion.url())
                .build());

        GuildMessageChannel channel = event.getMessage().getChannel().asGuildMessageChannel();
        if (!channel.canTalk()) {
            throw new BotErrorException("Missing permissions to talk in %s", channel.getAsMention());
        }

        channel.sendMessage(builder.build())
            .queue(callback -> reply.edit(Icon.OK, "Answer sent"));
    }

    private SelectOption @NotNull [] getOptions() {
        return this.questions.entrySet().stream()
            .map(entry -> {
                int id = entry.getKey();
                Question question = entry.getValue();
                SelectOption option = SelectOption.of(question.question(), String.valueOf(id));
                if (question.emoji() != null) {
                    option = option.withEmoji(Emoji.fromUnicode(question.emoji()));
                }
                if (question.description() != null) {
                    option = option.withDescription(question.description());
                }
                return option;
            })
            .sorted(Comparator.comparing(SelectOption::getLabel))
            .toArray(SelectOption[]::new);
    }

    @Contract("null -> new")
    private @NotNull RestAction<Message> updateMessage(Message message) {
        if (message == null || !message.getChannel().canTalk()) {
            return new CompletedRestAction<>(Bot.getInstance().getJDA(), null);
        }

        if (!this.questions.isEmpty()) {
            return message.editMessageComponents(
                    ActionRow.of(StringSelectMenu.create("faq_query")
                        .addOptions(getOptions())
                        .setPlaceholder("Select a question")
                        .setMaxValues(1)
                        .build()
                    )
                )
                .onErrorMap(e -> null);
        }
        return message.editMessageComponents().onErrorMap(e -> null);
    }

    @Override
    public String getStatus() {
        return String.format("""
                Enabled: %b
                Questions: %d
                Message: %s
                """,
            isEnabled(), this.questions.size(),
            Optional.ofNullable(faqMessage).map(Message::getJumpUrl).orElse("null"));
    }

    public record Question(int id,
                           String question,
                           String answer,
                           @Nullable String description,
                           @Nullable String emoji,
                           @Nullable String url) {

        public static Question fromJson(int id, String json) {
            JSONObject element = new JSONObject(json);
            String q = element.getString("q");
            String a = element.getString("a");
            String e = element.optString("e", null);
            String d = element.optString("d", null);
            String u = element.optString("u", null);
            return new Question(id, q, a, d, e, u);
        }

        public String toJson() {
            JSONObject object = new JSONObject()
                .put("q", question)
                .put("a", answer)
                .put("d", description)
                .put("e", emoji)
                .put("u", url);
            return object.toString();
        }

    }

}
