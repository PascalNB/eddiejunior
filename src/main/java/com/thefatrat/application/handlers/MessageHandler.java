package com.thefatrat.application.handlers;

import com.thefatrat.application.exceptions.BotException;
import com.thefatrat.application.util.Reply;
import net.dv8tion.jda.api.entities.Message;

import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;

public class MessageHandler implements Handler<Message> {

    private final Set<BiConsumer<Message, Reply>> list = new HashSet<>();

    public int size() {
        return list.size();
    }

    public void addListener(BiConsumer<Message, Reply> listener) {
        list.add(listener);
    }

    public void removeListener(BiConsumer<Message, Reply> listener) {
        list.remove(listener);
    }

    @Override
    public void handle(Message message, Reply reply) throws BotException {
        if (list.isEmpty()) {
            return;
        }

        for (BiConsumer<Message, Reply> listener : list) {
            listener.accept(message, reply);
        }
    }

}
