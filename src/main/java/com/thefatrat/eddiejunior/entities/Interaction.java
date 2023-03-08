package com.thefatrat.eddiejunior.entities;

import com.thefatrat.eddiejunior.events.InteractionEvent;
import com.thefatrat.eddiejunior.reply.EphemeralReply;
import com.thefatrat.eddiejunior.reply.ModalReply;
import com.thefatrat.eddiejunior.reply.Reply;
import org.jetbrains.annotations.Contract;

import java.util.function.BiConsumer;

public class Interaction<E> {

    private String name;
    private BiConsumer<InteractionEvent<E>, ?> action = (__, ___) -> {};

    public Interaction(String name) {this.name = name;}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @SuppressWarnings("unchecked")
    public <T extends Reply & EphemeralReply & ModalReply> BiConsumer<InteractionEvent<E>, T> getAction() {
        return (BiConsumer<InteractionEvent<E>, T>) action;
    }

    @Contract("_ -> this")
    public <T extends Reply & EphemeralReply & ModalReply> Interaction<E> setAction(
        BiConsumer<InteractionEvent<E>, T> action) {
        this.action = action;
        return this;
    }

}
