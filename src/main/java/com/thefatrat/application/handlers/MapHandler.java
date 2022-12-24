package com.thefatrat.application.handlers;

import com.thefatrat.application.entities.Reply;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

public class MapHandler<T> implements Handler<T> {

    private final Map<String, BiConsumer<T, Reply>> map = new HashMap<>();

    public void addListener(String key, BiConsumer<T, Reply> listener) {
        map.put(key, listener);
    }

    public void removeListener(String component) {
        map.remove(component);
    }

    public Set<String> getKeys() {
        return map.keySet();
    }

    public int size() {
        return map.size();
    }

    @Override
    public void handle(T t, Reply reply) {
        for (BiConsumer<T, Reply> listener : map.values()) {
            listener.accept(t, reply);
        }
    }

    public void handleOne(String key, T t, Reply reply) {
        BiConsumer<T, Reply> listener = map.get(key);
        if (listener != null) {
            listener.accept(t, reply);
        }
    }

}
