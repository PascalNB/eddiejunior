package com.thefatrat.application.handlers;

import com.thefatrat.application.entities.Reply;

import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;

public class SetHandler<T> implements Handler<T> {

    private final Set<BiConsumer<T, Reply>> set = new HashSet<>();

    public void addListener(BiConsumer<T, Reply> listener) {
        set.add(listener);
    }

    @Override
    public void handle(T t, Reply reply) {
        for (BiConsumer<T, Reply> listener : set) {
            listener.accept(t, reply);
        }
    }

}
