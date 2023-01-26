package com.thefatrat.eddiejunior.events;

import net.dv8tion.jda.api.entities.Message;

public class ButtonEvent<T> {

    private final T user;
    private final String buttonId;
    private final Message message;

    public ButtonEvent(T user, String buttonId, Message message) {
        this.user = user;
        this.buttonId = buttonId;
        this.message = message;
    }

    public T getUser() {
        return user;
    }

    public String getButtonId() {
        return buttonId;
    }

    public Message getMessage() {
        return message;
    }

}
