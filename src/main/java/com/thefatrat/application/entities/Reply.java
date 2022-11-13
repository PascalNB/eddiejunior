package com.thefatrat.application.entities;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditData;

import java.util.function.Consumer;

public interface Reply {

    void sendEmbed(MessageEmbed embed, Consumer<Message> callback);

    void sendMessageData(MessageCreateData data, Consumer<Message> callback);

    default void sendEmbed(MessageEmbed embed) {
        sendEmbed(embed, __ -> {});
    }

    default void sendMessageData(MessageCreateData data) {
        sendMessageData(data, __ -> {});
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
        return new Reply() {
            @Override
            public void sendEmbed(MessageEmbed embed, Consumer<Message> callback) {
                message.replyEmbeds(embed).queue(callback);
            }

            @Override
            public void sendMessageData(MessageCreateData data, Consumer<Message> callback) {
                message.reply(data).queue(callback);
            }
        };
    }

    static Reply defaultInteractionReply(InteractionHook hook) {
        return new Reply() {
            @Override
            public void sendEmbed(MessageEmbed embed, Consumer<Message> callback) {
                hook.editOriginalEmbeds(embed).queue(callback);
            }

            @Override
            public void sendMessageData(MessageCreateData data, Consumer<Message> callback) {
                hook.editOriginal(MessageEditData.fromCreateData(data)).queue(callback);
            }
        };
    }

    static Reply empty() {
        return new Reply() {
            @Override
            public void sendEmbed(MessageEmbed embed, Consumer<Message> callback) {}

            @Override
            public void sendMessageData(MessageCreateData data, Consumer<Message> callback) {}
        };
    }

    static Reply defaultMultiInteractionReply(InteractionHook hook) {
        return new Reply() {

            private boolean replied = false;

            @Override
            public void sendEmbed(MessageEmbed embed, Consumer<Message> callback) {
                if (!replied) {
                    replied = true;
                    hook.editOriginalEmbeds(embed).queue(callback);
                } else {
                    hook.getInteraction().getMessageChannel().sendMessageEmbeds(embed).queue(callback);
                }
            }

            @Override
            public void sendMessageData(MessageCreateData data, Consumer<Message> callback) {
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
