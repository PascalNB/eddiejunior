package com.thefatrat.application.entities;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.InteractionHook;

import java.util.function.Consumer;

public interface Reply {

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

    static Reply defaultMessageReply(Message message) {
        return (embed, callback) -> message.replyEmbeds(embed).queue(callback);
    }

    static Reply defaultInteractionReply(InteractionHook hook) {
        return (embed, callback) -> hook.editOriginalEmbeds(embed).queue(callback);
    }

}
