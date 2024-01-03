package com.thefatrat.eddiejunior.events;

import net.dv8tion.jda.api.entities.ScheduledEvent;

public class EventEvent {

    private final String name;
    private final String description;
    private final ScheduledEvent.Status status;
    private final ScheduledEvent.Status previousStatus;

    public EventEvent(String name, String description, ScheduledEvent.Status status,
        ScheduledEvent.Status previousStatus) {
        this.name = name;
        this.description = description;
        this.status = status;
        this.previousStatus = previousStatus;
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

    public ScheduledEvent.Status getPreviousStatus() {
        return previousStatus;
    }

}
