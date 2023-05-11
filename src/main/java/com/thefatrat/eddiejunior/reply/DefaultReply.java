package com.thefatrat.eddiejunior.reply;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

import java.util.function.Consumer;

public class DefaultReply implements Reply, EphemeralReply {

    private final IReplyCallback event;
    private InteractionHook action = null;
    private boolean ephemeral = false;

    public DefaultReply(IReplyCallback event) {
        this.event = event;
    }

    @Override
    public void send(MessageCreateData data, Consumer<Message> callback) {
        if (event.isAcknowledged()) {
            if (action == null) {
                event.getMessageChannel().sendMessage(data).queue(callback);
            } else {
                action.sendMessage(data).queue(callback);
                action = null;
            }
        } else {
            event.reply(data).setEphemeral(ephemeral).queue(hook -> hook.retrieveOriginal().queue(callback));
        }
    }

    @Override
    public void defer() {
        action = event.deferReply(ephemeral).complete();
    }

    @Override
    public void hide() {
        this.ephemeral = true;
    }

}
