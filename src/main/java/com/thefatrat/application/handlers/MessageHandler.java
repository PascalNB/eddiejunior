package com.thefatrat.application.handlers;

import net.dv8tion.jda.api.entities.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class MessageHandler implements Handler<Message> {

    private final List<Consumer<Message>> list = new ArrayList<>();

    public void addListener(Consumer<Message> listener) {
        list.add(listener);
    }

    public void removeListener(Consumer<Message> listener) {
        list.remove(listener);
    }

    @Override
    public boolean handle(Message message) {
        if (list.size() == 0) {
            return false;
        }

        for (Consumer<Message> listener : list) {
            listener.accept(message);
        }
        return true;
    }

}
