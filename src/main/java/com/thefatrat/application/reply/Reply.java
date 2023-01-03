package com.thefatrat.application.reply;

import com.thefatrat.application.exceptions.BotException;
import com.thefatrat.application.util.Colors;
import com.thefatrat.application.util.Icon;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.jetbrains.annotations.NotNull;

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

    default void send(Consumer<Message> callback, String content, Object... variables) {
        send(callback, Colors.TRANSPARENT, content, variables);
    }

    default void send(String content, Object... variables) {
        send(__ -> {}, content, variables);
    }

    default void send(Consumer<Message> callback, int color, String content, Object... variables) {
        send(new EmbedBuilder()
            .setColor(color)
            .setDescription(String.format(content, variables))
            .build(), callback);
    }

    default void send(int color, String content, Object... variables) {
        send(__ -> {}, color, content, variables);
    }

    default void send(@NotNull Icon icon, String content, Object... variables) {
        send(__ -> {}, icon.getColor(), icon + " " + content, variables);
    }

    default void send(Consumer<Message> callback, @NotNull Icon icon, String content, Object... variables) {
        send(callback, icon.getColor(), icon + " " + content, variables);
    }

    default void ok(Consumer<Message> callback, String content, Object... variables) {
        send(callback, Icon.OK, content, variables);
    }

    default void ok(String content, Object... variables) {
        ok(__ -> {}, content, variables);
    }

    default void send(@NotNull BotException exception) {
        send(exception.getColor(), exception.getMessage());
    }

    Reply EMPTY = (data, callback) -> {};

}