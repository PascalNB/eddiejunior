package com.thefatrat.application.handlers;

import com.thefatrat.application.entities.Reply;
import com.thefatrat.application.events.InteractionEvent;
import com.thefatrat.application.exceptions.BotErrorException;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class InteractionHandler implements Handler<InteractionEvent> {

    private final Map<String, BiConsumer<InteractionEvent, Reply>> map = new HashMap<>();

    public void addListener(String key, BiConsumer<InteractionEvent, Reply> listener) {
        map.put(key, listener);
    }

    @Override
    public void handle(InteractionEvent event, Reply reply) {
        if (!map.containsKey(event.getAction())) {
            throw new BotErrorException("Could not perform action");
        }
        map.get(event.getAction()).accept(event, reply);
    }

}
