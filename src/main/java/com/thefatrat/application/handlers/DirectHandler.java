package com.thefatrat.application.handlers;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DirectHandler {

    private final Map<String, MutualUser> map = new HashMap<>();

    public boolean contains(String id) {
        return map.containsKey(id);
    }

    public void removeUser(String id) {
        map.remove(id);
    }

    public List<Guild> getMutual(String id) {
        return map.get(id).mutual();
    }

    public Message getMessage(String id) {
        return map.get(id).message();
    }

    public void addUser(User user, List<Guild> mutual, Message message) {
        map.put(user.getId(), new MutualUser(user, mutual, message));
    }

    private record MutualUser(User user, List<Guild> mutual, Message message) {
    }

}
