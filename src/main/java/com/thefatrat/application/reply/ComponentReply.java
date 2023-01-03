package com.thefatrat.application.reply;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback;
import net.dv8tion.jda.api.interactions.callbacks.IModalCallback;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditData;

import java.util.function.Consumer;

public class ComponentReply<T extends IModalCallback & IReplyCallback & IMessageEditCallback>
    implements Reply, EditReply, ModalReply, EphemeralReply {

    private final T event;
    private final InteractionReply<T> reply;

    public ComponentReply(T event) {
        this.event = event;
        this.reply = new InteractionReply<>(event);
    }

    @Override
    public void edit(MessageEditData data, Consumer<Message> callback) {
        event.editMessage(data).queue(hook -> hook.retrieveOriginal().queue(callback));
    }

    @Override
    public void sendModal(Modal modal) {
        this.reply.sendModal(modal);
    }

    @Override
    public void send(MessageCreateData data, Consumer<Message> callback) {
        this.reply.send(data, callback);
    }

    @Override
    public void hide() {
        this.reply.hide();
    }

}
