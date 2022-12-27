package com.thefatrat.application.handlers;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

public class MapHandler<T, R> {

    private final Map<String, BiConsumer<T, R>> map = new HashMap<>();

    public void addListener(String key, BiConsumer<T, R> listener) {
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

    public void handleAll(T t, R reply) {
        for (BiConsumer<T, R> listener : map.values()) {
            listener.accept(t, reply);
        }
    }

    public void handle(String key, T t, R reply) {
        BiConsumer<T, R> listener = map.get(key);
        if (listener != null) {
            listener.accept(t, reply);
        }
    }

}
