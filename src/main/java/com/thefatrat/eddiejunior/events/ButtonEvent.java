package com.thefatrat.eddiejunior.events;

import net.dv8tion.jda.api.entities.Message;

public class ButtonEvent<T> {

    private final T actor;
    private final String buttonId;
    private final Message message;

    public ButtonEvent(T actor, String buttonId, Message message) {
        this.actor = actor;
        this.buttonId = buttonId;
        this.message = message;
    }

    public T getActor() {
        return actor;
    }

    public String getButtonId() {
        return buttonId;
    }

    public Message getMessage() {
        return message;
    }

}
