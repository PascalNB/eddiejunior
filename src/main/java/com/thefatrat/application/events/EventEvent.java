package com.thefatrat.application.events;

import net.dv8tion.jda.api.entities.ScheduledEvent;

public class EventEvent {

    private final String name;
    private final String description;
    private final ScheduledEvent.Status status;

    public EventEvent(String name, String description, ScheduledEvent.Status status) {
        this.name = name;
        this.description = description;
        this.status = status;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public ScheduledEvent.Status getStatus() {
        return status;
    }

}
