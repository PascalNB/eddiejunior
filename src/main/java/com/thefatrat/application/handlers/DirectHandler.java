package com.thefatrat.application.handlers;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;

import java.util.HashMap;
import java.util.Map;

public class DirectHandler {

    private final Map<String, MutualUser> map = new HashMap<>();

    public boolean contains(String id) {
        return map.containsKey(id);
    }

    public void removeUser(String id) {
        map.remove(id);
    }

    public Message getMessage(String id) {
        return map.get(id).message();
    }

    public void addUser(User user, Message message) {
        map.put(user.getId(), new MutualUser(user, message));
    }

    private record MutualUser(User user, Message message) {
    }

}
