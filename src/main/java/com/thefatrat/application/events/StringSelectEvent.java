package com.thefatrat.application.events;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;

public class StringSelectEvent {

    private final User user;
    private final Message message;
    private final String menuId;
    private final String option;

    public StringSelectEvent(User user, Message message, String menuId, String option) {
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

    public String getOption() {
        return option;
    }

}
