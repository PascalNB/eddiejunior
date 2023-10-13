package com.thefatrat.eddiejunior.handlers;

import com.thefatrat.eddiejunior.components.Component;
import com.thefatrat.eddiejunior.reply.MenuReply;
import net.dv8tion.jda.api.entities.Message;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

public class ComponentHandler {

    private final Map<String, String> names = new HashMap<>();
    private final MapHandler<Message, MenuReply> handler = new MapHandler<>();

    public Set<Map.Entry<String, String>> getNames() {
        return names.entrySet();
    }

    public Set<String> getComponents() {
        return handler.getKeys();
    }

    public void handle(String component, Message message, MenuReply reply) {
        handler.handle(component, message, reply);
    }

    public void addListener(Component component, String alt, BiConsumer<Message, MenuReply> listener) {
        handler.addListener(component.getId(), listener);
        names.put(component.getId(), alt);
    }

    public void removeListener(Component component) {
        handler.removeListener(component.getId());
        names.remove(component.getId());
    }

}
