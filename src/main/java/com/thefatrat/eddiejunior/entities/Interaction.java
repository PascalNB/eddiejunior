package com.thefatrat.eddiejunior.entities;

import com.thefatrat.eddiejunior.events.MessageInteractionEvent;
import com.thefatrat.eddiejunior.reply.EphemeralReply;
import com.thefatrat.eddiejunior.reply.ModalReply;
import com.thefatrat.eddiejunior.reply.Reply;
import org.jetbrains.annotations.Contract;

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

    @Contract("_ -> this")
    public <T extends Reply & EphemeralReply & ModalReply> Interaction setAction(
        BiConsumer<MessageInteractionEvent, T> action) {
        this.action = action;
        return this;
    }

}
