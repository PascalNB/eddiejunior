package com.thefatrat.application.entities;

import com.thefatrat.application.events.InteractionEvent;

import java.util.function.BiConsumer;

public class Interaction {

    private final String name;
    private BiConsumer<InteractionEvent, Reply> action = (__, ___) -> {};

    public Interaction(String name) {this.name = name;}

    public String getName() {
        return name;
    }

    public BiConsumer<InteractionEvent, Reply> getAction() {
        return action;
    }

    public Interaction setAction(BiConsumer<InteractionEvent, Reply> action) {
        this.action = action;
        return this;
    }

}
