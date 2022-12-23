package com.thefatrat.application.handlers;

import com.thefatrat.application.entities.Reply;
import com.thefatrat.application.exceptions.BotException;
import net.dv8tion.jda.api.entities.Message;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

public class MessageHandler implements Handler<Message> {

    private final Map<String, BiConsumer<Message, Reply>> map = new HashMap<>();

    public int size() {
        return map.size();
    }

    public void addListener(String component, BiConsumer<Message, Reply> listener) {
        map.put(component, listener);
    }

    public void removeListener(String component) {
        map.remove(component);
    }

    public Set<String> getKeys() {
        return map.keySet();
    }

    @Override
    public void handle(Message message, Reply reply) throws BotException {
        for (BiConsumer<Message, Reply> listener : map.values()) {
            listener.accept(message, reply);
        }
    }

    public void handle(String component, Message message, Reply reply) {
        BiConsumer<Message, Reply> listener = map.get(component);
        if (listener != null) {
            listener.accept(message, reply);
        }
    }

}
