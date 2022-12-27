package com.thefatrat.application.reply;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditData;

import java.util.function.Consumer;

public class EditReply implements Reply {

    private final IMessageEditCallback event;

    public EditReply(IMessageEditCallback event) {
        this.event = event;
    }

    @Override
    public void send(MessageCreateData data, Consumer<Message> callback) {
        event.editMessage(MessageEditData.fromCreateData(data))
            .queue(hook -> hook.retrieveOriginal().queue(callback));
    }

    @Override
    public Reply defer(boolean ephemeral) {
        return this;
    }

    @Override
    public Reply hide() {
        return this;
    }

}
