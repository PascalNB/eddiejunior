package com.thefatrat.application.reply;

import com.thefatrat.application.exceptions.BotException;
import com.thefatrat.application.util.Colors;
import com.thefatrat.application.util.Icon;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

import java.util.function.Consumer;

public interface Reply {

    void accept(MessageCreateData data, Consumer<Message> callback);

    void accept(Modal modal);

    Reply defer(boolean ephemeral);

    Reply hide();

    default void accept(MessageEmbed embed, Consumer<Message> callback) {
        accept(MessageCreateData.fromEmbeds(embed), callback);
    }

    default void accept(MessageEmbed embed) {
        accept(embed, __ -> {});
    }

    default void accept(MessageCreateData data) {
        accept(data, __ -> {});
    }

    default void accept(Consumer<Message> callback, String content, Object... variables) {
        accept(callback, Colors.TRANSPARENT, content, variables);
    }

    default void accept(String content, Object... variables) {
        accept(__ -> {}, content, variables);
    }

    default void accept(Consumer<Message> callback, int color, String content, Object... variables) {
        accept(new EmbedBuilder()
            .setColor(color)
            .setDescription(String.format(content, variables))
            .build(), callback);
    }

    default void accept(int color, String content, Object... variables) {
        accept(__ -> {}, color, content, variables);
    }

    default void accept(Icon icon, String content, Object... variables) {
        accept(__ -> {}, icon.getColor(), icon + " " + content, variables);
    }

    default void accept(Consumer<Message> callback, Icon icon, String content, Object... variables) {
        accept(callback, icon.getColor(), icon + " " + content, variables);
    }

    default void ok(Consumer<Message> callback, String content, Object... variables) {
        accept(callback, Icon.OK, content, variables);
    }

    default void ok(String content, Object... variables) {
        ok(__ -> {}, content, variables);
    }

    default void except(BotException exception) {
        accept(exception.getColor(), exception.getMessage());
    }

    static Reply defaultMessageReply(Message message) {
        return new Reply() {
            @Override
            public void accept(MessageCreateData data, Consumer<Message> callback) {
                message.reply(data).queue(callback);
            }

            @Override
            public void accept(Modal modal) {
                throw new UnsupportedOperationException("Cannot reply to a message with a modal");
            }

            @Override
            public Reply defer(boolean ephemeral) {
                return this;
            }

            @Override
            public Reply hide() {
                return this;
            }

        };
    }

    static Reply empty() {
        return new Reply() {
            @Override
            public void accept(MessageCreateData data, Consumer<Message> callback) {
            }

            @Override
            public void accept(Modal modal) {
            }

            @Override
            public Reply defer(boolean ephemeral) {
                return this;
            }

            @Override
            public Reply hide() {
                return this;
            }
        };
    }

}
