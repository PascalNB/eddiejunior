package com.thefatrat.application.handlers;

import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;

public class SetHandler<T, R> implements Handler<T, R> {

    private final Set<BiConsumer<T, R>> set = new HashSet<>();

    public void addListener(BiConsumer<T, R> listener) {
        set.add(listener);
    }

    @Override
    public void handle(T t, R reply) {
        for (BiConsumer<T, R> listener : set) {
            listener.accept(t, reply);
        }
    }

}
