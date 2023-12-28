package com.thefatrat.eddiejunior.handlers;

import com.thefatrat.eddiejunior.components.Component;
import com.thefatrat.eddiejunior.events.GenericEvent;
import com.thefatrat.eddiejunior.reply.MenuReply;

import java.util.function.BiConsumer;

public class RequestHandler {

    private final MapHandler<GenericEvent<?>, MenuReply> handler = new MapHandler<>();

    public void handle(String requestId, GenericEvent<?> event, MenuReply reply) {
        handler.handle(requestId, event, reply);
    }

    public <T> void addListener(Component component, String requestId,
        BiConsumer<GenericEvent<T>, MenuReply> listener) {
        // noinspection unchecked,rawtypes
        handler.addListener(component.getId() + "-" + requestId, (BiConsumer) listener);
    }

}
