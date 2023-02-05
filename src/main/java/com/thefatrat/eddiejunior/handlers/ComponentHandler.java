package com.thefatrat.eddiejunior.handlers;

import com.thefatrat.eddiejunior.reply.EditReply;
import com.thefatrat.eddiejunior.reply.Reply;
import net.dv8tion.jda.api.entities.Message;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

public class ComponentHandler<R extends Reply & EditReply> extends MapHandler<Message, R> {

    private final Map<String, String> names = new HashMap<>();

    public Set<Map.Entry<String, String>> getNames() {
        return names.entrySet();
    }

    public void addListener(String key, String name, BiConsumer<Message, R> listener) {
        super.addListener(key, listener);
        names.put(key, name);
    }

    @Override
    public void removeListener(String key) {
        super.removeListener(key);
        names.remove(key);
    }

}
