package com.thefatrat.application.util;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.util.function.Consumer;

public interface Reply {

    default void sendMessage(String message) {
        sendMessage(message, __ -> {});
    }

    default void sendMessageFormat(String message, Object... args) {
        sendMessage(String.format(message, args));
    }

    void sendMessage(String message, Consumer<Message> callback);

    void sendEmbed(MessageEmbed embed, Consumer<Message> callback);

    default void sendEmbed(MessageEmbed embed) {
        sendEmbed(embed, __ -> {});
    }

    default void sendEmbedFormat(Consumer<Message> callback, int color, String content,
        Object... variables) {
        sendEmbed(new EmbedBuilder()
            .setColor(color)
            .setDescription(String.format(content, variables))
            .build(), callback);
    }

    default void sendEmbedFormat(int color, String content, Object... variables) {
        sendEmbedFormat(__ -> {}, color, content, variables);
    }

}
