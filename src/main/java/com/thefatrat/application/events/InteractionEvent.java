package com.thefatrat.application.events;

import net.dv8tion.jda.api.entities.Message;

public class InteractionEvent {

    private final Message message;
    private final String action;

    public InteractionEvent(Message message, String action) {
        this.message = message;
        this.action = action;
    }

    public Message getMessage() {
        return message;
    }

    public String getAction() {
        return action;
    }

}
