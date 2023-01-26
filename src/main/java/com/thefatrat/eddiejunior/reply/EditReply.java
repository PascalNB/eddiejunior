package com.thefatrat.eddiejunior.reply;

import com.thefatrat.eddiejunior.exceptions.BotException;
import com.thefatrat.eddiejunior.util.Icon;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditData;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public interface EditReply {

    void edit(MessageEditData data, Consumer<Message> callback);

    default void edit(MessageCreateData data) {
        edit(data, m -> {});
    }

    default void edit(MessageCreateData data, Consumer<Message> callback) {
        edit(MessageEditData.fromCreateData(data), callback);
    }

    default void edit(MessageEditData data) {
        edit(data, m -> {});
    }

    default void edit(MessageEmbed embed) {
        edit(MessageCreateData.fromEmbeds(embed));
    }

    default void edit(@NotNull Icon icon, String content, Object... variables) {
        edit(new EmbedBuilder()
            .setColor(icon.getColor())
            .setDescription(icon + " " + String.format(content, variables))
            .build()
        );
    }

    default void edit(@NotNull BotException e) {
        edit(new EmbedBuilder()
            .setColor(e.getColor())
            .setDescription(e.getMessage())
            .build()
        );
    }

}
