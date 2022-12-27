package com.thefatrat.application.entities;

import com.thefatrat.application.events.MessageInteractionEvent;
import com.thefatrat.application.reply.Reply;

import java.util.function.BiConsumer;

public class Interaction {

    private String name;
    private BiConsumer<MessageInteractionEvent, Reply> action = (__, ___) -> {};

    public Interaction(String name) {this.name = name;}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BiConsumer<MessageInteractionEvent, Reply> getAction() {
        return action;
    }

    public Interaction setAction(BiConsumer<MessageInteractionEvent, Reply> action) {
        this.action = action;
        return this;
    }

}
