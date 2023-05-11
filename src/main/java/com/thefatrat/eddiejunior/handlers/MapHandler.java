package com.thefatrat.eddiejunior.handlers;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

public class MapHandler<T, R> {

    private final Map<String, BiConsumer<T, R>> map = new HashMap<>();

    public void addListener(String key, BiConsumer<T, R> listener) {
        map.put(key, listener);
    }

    public void removeListener(String key) {
        map.remove(key);
    }

    public Set<String> getKeys() {
        return map.keySet();
    }

    public void handle(String key, T t, R reply) {
        BiConsumer<T, R> listener = map.get(key);
        if (listener != null) {
            listener.accept(t, reply);
        }
    }

}
