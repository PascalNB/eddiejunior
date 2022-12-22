package com.thefatrat.application.handlers;

import com.thefatrat.application.entities.Reply;
import com.thefatrat.application.events.ButtonEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;

public class ButtonHandler implements Handler<ButtonEvent> {

    private final Set<BiConsumer<ButtonEvent, Reply>> set = new HashSet<>();

    public void addListener(BiConsumer<ButtonEvent, Reply> listener) {
        set.add(listener);
    }

    @Override
    public void handle(ButtonEvent event, Reply reply) {
        for (BiConsumer<ButtonEvent, Reply> listener : set) {
            listener.accept(event, reply);
        }
    }

}
