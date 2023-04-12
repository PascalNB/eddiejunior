package com.thefatrat.eddiejunior.events;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;

public class SelectEvent<T> {

    private final User user;
    private final Message message;
    private final String menuId;
    private final T option;

    public SelectEvent(User user, Message message, String menuId, T option) {
        this.user = user;
        this.message = message;
        this.menuId = menuId;
        this.option = option;
    }

    public User getUser() {
        return user;
    }

    public Message getMessage() {
        return message;
    }

    public String getMenuId() {
        return menuId;
    }

    public T getOption() {
        return option;
    }

}
