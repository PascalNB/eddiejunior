package com.thefatrat.eddiejunior;

import java.util.AbstractMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class TimedMap<K, V> extends AbstractMap<K, V> {

    private final Map<K, Map.Entry<V, Long>> map;

    public TimedMap(long time) {
        this.map = new LinkedHashMap<>(16, 0.75f, true) {

            @Override
            protected boolean removeEldestEntry(Map.Entry<K, Map.Entry<V, Long>> eldest) {
                return (System.currentTimeMillis() - eldest.getValue().getValue()) > time;
            }

        };
    }

    @Override
    public V put(K key, V value) {
        Map.Entry<V, Long> v = map.put(key, Map.entry(value, System.currentTimeMillis()));
        return v == null ? null : v.getKey();
    }

    @Override
    public V get(Object key) {
        Map.Entry<V, Long> v = map.get(key);
        return v == null ? null : v.getKey();
    }

    @Override
    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return map.entrySet().stream()
            .map(entry -> Map.entry(entry.getKey(), entry.getValue().getKey()))
            .collect(Collectors.toSet());
    }

}
