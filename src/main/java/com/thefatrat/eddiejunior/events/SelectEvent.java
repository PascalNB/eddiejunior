package com.thefatrat.eddiejunior.events;

import com.thefatrat.eddiejunior.util.MetadataHolder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;

import java.util.Map;

public class SelectEvent<T> implements MetadataHolder {

    private final String id;
    private final User user;
    private final Message message;
    private final T option;
    private Map<String, Object> metadata;

    public SelectEvent(String id, User user, Message message, T option) {
        this.id = id;
        this.user = user;
        this.message = message;
        this.option = option;
    }

    public User getUser() {
        return user;
    }

    public Message getMessage() {
        return message;
    }

    public T getOption() {
        return option;
    }

    @Override
    public String getMetadataId() {
        return id;
    }

    @Override
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    @Override
    public Map<String, Object> getMetadata() {
        return metadata;
    }

}
