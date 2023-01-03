package com.thefatrat.application.entities;

import com.thefatrat.application.events.MessageInteractionEvent;
import com.thefatrat.application.reply.EphemeralReply;
import com.thefatrat.application.reply.ModalReply;
import com.thefatrat.application.reply.Reply;

import java.util.function.BiConsumer;

public class Interaction {

    private String name;
    private BiConsumer<MessageInteractionEvent, ?> action = (__, ___) -> {};

    public Interaction(String name) {this.name = name;}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @SuppressWarnings("unchecked")
    public <T extends Reply & EphemeralReply & ModalReply> BiConsumer<MessageInteractionEvent, T> getAction() {
        return (BiConsumer<MessageInteractionEvent, T>) action;
    }

    public <T extends Reply & EphemeralReply & ModalReply> Interaction setAction(
        BiConsumer<MessageInteractionEvent, T> action) {
        this.action = action;
        return this;
    }

}
