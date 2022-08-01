package com.thefatrat.application.handlers;

import com.thefatrat.application.exceptions.BotException;
import net.dv8tion.jda.api.entities.Message;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class MessageHandler implements Handler<Message> {

    private final Set<Consumer<Message>> list = new HashSet<>();

    public int size() {
        return list.size();
    }

    public void addListener(Consumer<Message> listener) {
        list.add(listener);
    }

    public void removeListener(Consumer<Message> listener) {
        list.remove(listener);
    }

    @Override
    public void handle(Message message) throws BotException {
        if (list.size() == 0) {
            return;
        }

        for (Consumer<Message> listener : list) {
            listener.accept(message);
        }
    }

}
