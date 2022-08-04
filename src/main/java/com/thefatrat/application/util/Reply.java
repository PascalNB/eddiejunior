package com.thefatrat.application.util;

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

    void sendEmbed(MessageEmbed embed);

}
