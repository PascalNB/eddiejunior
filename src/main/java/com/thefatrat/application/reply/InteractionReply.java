package com.thefatrat.application.reply;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditData;

import java.util.function.Consumer;

public class InteractionReply implements Reply {

    private final IReplyCallback event;

    public InteractionReply(IReplyCallback event) {
        this.event = event;
    }

    private boolean ephemeral = false;
    private boolean replied = false;
    private InteractionHook hook = null;

    @Override
    public void accept(MessageCreateData data, Consumer<Message> callback) {
        if (!replied) {
            replied = true;
            if (hook != null) {
                hook.editOriginal(MessageEditData.fromCreateData(data)).queue(callback);
                return;
            }
            event.reply(data).setEphemeral(ephemeral).queue(hook -> hook.retrieveOriginal().queue(callback));
        } else {
            event.getMessageChannel().sendMessage(data).queue(callback);
        }
    }

    @Override
    public Reply defer(boolean ephemeral) {
        if (hook == null) {
            hook = event.deferReply(ephemeral).complete();
        }
        return this;
    }

    @Override
    public Reply hide() {
        ephemeral = true;
        return this;
    }

}
