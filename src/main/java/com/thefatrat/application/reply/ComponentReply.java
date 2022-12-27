package com.thefatrat.application.reply;

import net.dv8tion.jda.api.interactions.components.ComponentInteraction;

public class ComponentReply {

    private final Reply sender;
    private final Reply editor;

    public ComponentReply(ComponentInteraction event) {
        sender = new InteractionReply(event);
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
