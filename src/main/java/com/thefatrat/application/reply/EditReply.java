package com.thefatrat.application.reply;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditData;

import java.util.function.Consumer;

public class EditReply implements Reply {

    private final IMessageEditCallback event;

    public EditReply(IMessageEditCallback event) {
        this.event = event;
    }

    @Override
    public void accept(MessageCreateData data, Consumer<Message> callback) {
        event.editMessage(MessageEditData.fromCreateData(data))
            .queue(hook -> hook.retrieveOriginal().queue(callback));
    }

    @Override
    public void accept(Modal modal) {
        throw new UnsupportedOperationException("Cannot edit a message with a modal");
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
