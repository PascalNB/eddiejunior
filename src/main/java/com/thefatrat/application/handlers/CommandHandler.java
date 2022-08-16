package com.thefatrat.application.handlers;

import com.thefatrat.application.entities.Reply;
import com.thefatrat.application.events.CommandEvent;
import com.thefatrat.application.exceptions.BotException;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class CommandHandler implements Handler<CommandEvent> {

    private final Map<String, BiConsumer<CommandEvent, Reply>> map = new HashMap<>();

    public void addListener(String key, BiConsumer<CommandEvent, Reply> listener) {
        map.put(key.toLowerCase(), listener);
    }

    public void removeListener(String key) {
        map.remove(key);
    }

    @Override
    public void handle(CommandEvent command, Reply reply) throws BotException {
        BiConsumer<CommandEvent, Reply> listener = map.get(command.getCommand());
        if (listener == null) {
            return;
        }
        listener.accept(command, reply);
    }

}
