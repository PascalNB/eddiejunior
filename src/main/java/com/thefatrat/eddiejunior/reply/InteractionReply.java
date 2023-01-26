package com.thefatrat.eddiejunior.reply;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.callbacks.IModalCallback;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

import java.util.function.Consumer;

public class InteractionReply<T extends IReplyCallback & IModalCallback> implements Reply, ModalReply, EphemeralReply {

    private final T event;
    private final GenericReply<T> reply;

    public InteractionReply(T event) {
        this.event = event;
        this.reply = new GenericReply<>(event);
    }

    @Override
    public void send(MessageCreateData data, Consumer<Message> callback) {
        reply.send(data, callback);
    }

    @Override
    public void sendModal(Modal modal) {
        if (event.isAcknowledged()) {
            throw new UnsupportedOperationException("Can only reply with a modal once");
        }
        event.replyModal(modal).queue();
    }

    @Override
    public void hide() {
        reply.hide();
    }

}
