package com.thefatrat.application.handlers;

import com.thefatrat.application.entities.Reply;
import com.thefatrat.application.events.MessageInteractionEvent;
import com.thefatrat.application.exceptions.BotErrorException;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class MessageInteractionHandler implements Handler<MessageInteractionEvent> {

    private final Map<String, BiConsumer<MessageInteractionEvent, Reply>> map = new HashMap<>();

    public void addListener(String key, BiConsumer<MessageInteractionEvent, Reply> listener) {
        map.put(key, listener);
    }

    @Override
    public void handle(MessageInteractionEvent event, Reply reply) {
        BiConsumer<MessageInteractionEvent, Reply> action = map.get(event.getAction());
        if (action == null) {
            throw new BotErrorException("Could not perform action");
        }
        action.accept(event, reply);
    }

}
