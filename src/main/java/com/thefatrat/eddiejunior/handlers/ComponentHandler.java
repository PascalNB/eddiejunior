package com.thefatrat.eddiejunior.handlers;

import com.thefatrat.eddiejunior.reply.MenuReply;
import net.dv8tion.jda.api.entities.Message;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

public class ComponentHandler extends MapHandler<Message, MenuReply> {

    private final Map<String, String> names = new HashMap<>();

    public Set<Map.Entry<String, String>> getNames() {
        return names.entrySet();
    }

    @Override
    public void addListener(String key, BiConsumer<Message, MenuReply> listener) {
        this.addListener(key, key, listener);
    }

    public void addListener(String key, String name, BiConsumer<Message, MenuReply> listener) {
        super.addListener(key, listener);
        names.put(key, name);
    }

    @Override
    public void removeListener(String key) {
        super.removeListener(key);
        names.remove(key);
    }

}
