package com.thefatrat.eddiejunior.reply;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

import java.util.function.Consumer;

public class GenericReply implements Reply, EphemeralReply {

    private final IReplyCallback event;
    private boolean ephemeral = false;

    public GenericReply(IReplyCallback event) {
        this.event = event;
    }

    @Override
    public void send(MessageCreateData data, Consumer<Message> callback) {
        if (event.isAcknowledged()) {
            event.getMessageChannel().sendMessage(data).queue(callback);
        } else {
            event.reply(data).setEphemeral(ephemeral).queue(hook -> hook.retrieveOriginal().queue(callback));
        }
    }

    @Override
    public void hide() {
        this.ephemeral = true;
    }

}
