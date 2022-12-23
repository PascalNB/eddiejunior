package com.thefatrat.application.entities;

import com.thefatrat.application.exceptions.BotException;
import com.thefatrat.application.util.Icon;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditData;

import java.util.function.Consumer;

public interface Reply {

    void send(MessageCreateData data, Consumer<Message> callback);

    default void send(MessageEmbed embed, Consumer<Message> callback) {
        send(MessageCreateData.fromEmbeds(embed), callback);
    }

    default void send(MessageEmbed embed) {
        send(embed, __ -> {});
    }

    default void send(MessageCreateData data) {
        send(data, __ -> {});
    }

    default void send(Consumer<Message> callback, int color, String content,
        Object... variables) {
        send(new EmbedBuilder()
            .setColor(color)
            .setDescription(String.format(content, variables))
            .build(), callback);
    }

    default void send(int color, String content, Object... variables) {
        send(__ -> {}, color, content, variables);
    }

    default void send(Icon icon, String content, Object... variables) {
        send(__ -> {}, icon.getColor(), icon + " " + content, variables);
    }

    default void send(Consumer<Message> callback, Icon icon, String content, Object... variables) {
        send(callback, icon.getColor(), icon + " " + content, variables);
    }

    default void ok(Consumer<Message> callback, String content, Object... variables) {
        send(callback, Icon.OK, content, variables);
    }

    default void ok(String content, Object... variables) {
        ok(__ -> {}, content, variables);
    }

    default void except(BotException exception) {
        send(exception.getColor(), exception.getMessage());
    }

    static Reply defaultMessageReply(Message message) {
        return (data, callback) -> message.reply(data).queue(callback);
    }

    static Reply defaultInteractionReply(InteractionHook hook) {
        return (data, callback) -> hook.editOriginal(MessageEditData.fromCreateData(data)).queue(callback);
    }

    static Reply empty() {
        return (data, callback) -> {};
    }

    static Reply immediateMultiInteractionReply(IReplyCallback event) {
        return new Reply() {

            private boolean replied = false;

            @Override
            public void send(MessageCreateData data, Consumer<Message> callback) {
                if (!replied) {
                    replied = true;
                    event.reply(data).queue(hook -> hook.retrieveOriginal().queue(callback));
                } else {
                    event.getMessageChannel().sendMessage(data).queue(callback);
                }
            }
        };
    }

    @SuppressWarnings("unused")
    static Reply defaultMultiInteractionReply(InteractionHook hook) {
        return new Reply() {

            private boolean replied = false;

            @Override
            public void send(MessageCreateData data, Consumer<Message> callback) {
                if (!replied) {
                    replied = true;
                    hook.editOriginal(MessageEditData.fromCreateData(data)).queue(callback);
                } else {
                    hook.getInteraction().getMessageChannel().sendMessage(data).queue(callback);
                }
            }
        };
    }

}
