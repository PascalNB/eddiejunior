package com.thefatrat.eddiejunior.events;

import net.dv8tion.jda.api.entities.User;

public class RequestEvent<T> {

    private final User user;
    private final T t;

    public RequestEvent(User user, T t) {
        this.user = user;
        this.t = t;
    }

    public User getUser() {
        return user;
    }

    public T getData() {
        return t;
    }

}
