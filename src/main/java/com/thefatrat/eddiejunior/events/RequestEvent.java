package com.thefatrat.eddiejunior.events;

import net.dv8tion.jda.api.entities.User;

public class RequestEvent {

    private final User user;

    public RequestEvent(User user) {
        this.user = user;
    }

    public User getUser() {
        return user;
    }

}
