package com.thefatrat.eddiejunior.reply;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback;
import net.dv8tion.jda.api.interactions.callbacks.IModalCallback;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditData;

import java.util.function.Consumer;

public class MenuReply implements Reply, EditReply, ModalReply, EphemeralReply {

    private final IMessageEditCallback messageEditCallback;
    private final InteractionReply reply;

    public <T extends IModalCallback & IReplyCallback & IMessageEditCallback> MenuReply(T callback) {
        this.messageEditCallback = callback;
        this.reply = new InteractionReply(callback);
    }

    @Override
    public synchronized void edit(MessageEditData data, Consumer<Message> callback) {
        messageEditCallback.editMessage(data).queue(hook -> hook.retrieveOriginal().queue(callback));
    }

    @Override
    public synchronized void sendModal(Modal modal) {
        this.reply.sendModal(modal);
    }

    @Override
    public synchronized void send(MessageCreateData data, Consumer<Message> callback) {
        this.reply.send(data, callback);
    }

    @Override
    public synchronized void defer() {
        this.reply.defer();
    }

    @Override
    public synchronized void hide() {
        this.reply.hide();
    }

}
