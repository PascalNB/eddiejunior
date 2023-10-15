package com.thefatrat.eddiejunior.handlers;

import com.thefatrat.eddiejunior.components.Component;
import com.thefatrat.eddiejunior.events.RequestEvent;
import com.thefatrat.eddiejunior.reply.MenuReply;

import java.util.function.BiConsumer;

public class RequestHandler {

    private final MapHandler<RequestEvent, MenuReply> handler = new MapHandler<>();

    public void handle(String requestId, RequestEvent event, MenuReply reply) {
        handler.handle(requestId, event, reply);
    }

    public void addListener(Component component, String requestId,
        BiConsumer<RequestEvent, MenuReply> listener) {

        handler.addListener(component.getId() + "-" + requestId, listener);
    }

}
