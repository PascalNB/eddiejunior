package com.thefatrat.application.handlers;

import com.thefatrat.application.entities.Reply;
import com.thefatrat.application.events.StringSelectEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;

public class StringSelectHandler implements Handler<StringSelectEvent> {

    private final Set<BiConsumer<StringSelectEvent, Reply>> set = new HashSet<>();

    public void addListener(BiConsumer<StringSelectEvent, Reply> listener) {
        set.add(listener);
    }

    @Override
    public void handle(StringSelectEvent event, Reply reply) {
        for (BiConsumer<StringSelectEvent, Reply> listener : set) {
            listener.accept(event, reply);
        }
    }

}
