package com.thefatrat.application.reply;

import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback;
import net.dv8tion.jda.api.interactions.callbacks.IModalCallback;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;

public class ComponentReply {

    private final Reply sender;
    private final Reply editor;

    public <T extends IModalCallback & IReplyCallback & IMessageEditCallback> ComponentReply(T event) {
        sender = new InteractionReply<>(event);
        editor = new EditReply(event);
    }

    private ComponentReply() {
        sender = Reply.empty();
        editor = Reply.empty();
    }

    public Reply getSender() {
        return sender;
    }

    public Reply getEditor() {
        return editor;
    }

    public static ComponentReply empty() {
        return new ComponentReply();
    }

}
