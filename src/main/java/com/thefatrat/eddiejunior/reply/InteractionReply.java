package com.thefatrat.eddiejunior.reply;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.callbacks.IModalCallback;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

import java.util.function.Consumer;

public class InteractionReply implements Reply, ModalReply, EphemeralReply {

    private final IModalCallback modalCallback;
    private final DefaultReply reply;

    public <T extends IReplyCallback & IModalCallback> InteractionReply(T callback) {
        this.modalCallback = callback;
        this.reply = new DefaultReply(callback);
    }

    @Override
    public synchronized void send(MessageCreateData data, Consumer<Message> callback) {
        reply.send(data, callback);
    }

    @Override
    public synchronized void defer() {
        reply.defer();
    }

    @Override
    public synchronized void sendModal(Modal modal) {
        if (modalCallback.isAcknowledged()) {
            throw new UnsupportedOperationException("Can only reply with a modal once");
        }
        modalCallback.replyModal(modal).queue();
    }

    @Override
    public synchronized void hide() {
        reply.hide();
    }

}
