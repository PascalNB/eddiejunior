package com.thefatrat.application.handlers;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public abstract class EventHandler<T> {

    private final Map<String, Consumer<T>> map = new HashMap<>();

    public EventHandler() {
        register();
    }

    protected abstract void register();

    public void addListener(String key, Consumer<T> listener) {
        map.put(key, listener);
    }

    public void removeListener(String key) {
        map.remove(key);
    }

    protected boolean execute(String key, T t) {
        Consumer<T> listener = map.get(key);
        if (listener == null) {
            return false;
        }
        listener.accept(t);
        return true;
    }

    public abstract boolean handle(T t);

}
