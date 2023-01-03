package com.thefatrat.application.reply;

import com.thefatrat.application.exceptions.BotException;
import com.thefatrat.application.util.Icon;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.utils.messages.MessageEditData;

import java.util.function.Consumer;

public interface EditReply {

    void edit(MessageEditData data, Consumer<Message> callback);

    default void edit(MessageEditData data) {
        edit(data, m -> {});
    }

    default void edit(MessageEmbed embed) {
        edit(MessageEditData.fromEmbeds(embed));
    }

    default void edit(Icon icon, String content, Object... variables) {
        edit(new EmbedBuilder()
            .setColor(icon.getColor())
            .setDescription(icon + " " + String.format(content, variables))
            .build()
        );
    }

    default void edit(BotException e) {
        edit(new EmbedBuilder()
            .setColor(e.getColor())
            .setDescription(e.getMessage())
            .build()
        );
    }

}
