package com.thefatrat.eddiejunior.reply;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

import java.util.function.Consumer;

public class MessageReply implements Reply {

    private final Message message;

    public MessageReply(Message message) {
        this.message = message;
    }

    @Override
    public synchronized void send(MessageCreateData data, Consumer<Message> callback) {
        message.reply(data).queue(callback);
    }

    @Override
    public synchronized void defer() {

    }

}
