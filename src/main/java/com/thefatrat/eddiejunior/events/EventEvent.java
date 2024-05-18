package com.thefatrat.eddiejunior.events;

import net.dv8tion.jda.api.entities.ScheduledEvent;
import net.dv8tion.jda.api.entities.channel.Channel;

public class EventEvent {

    private final String name;
    private final String description;
    private final ScheduledEvent.Status status;
    private final ScheduledEvent.Status previousStatus;
    private final Channel channel;

    public EventEvent(String name, String description, ScheduledEvent.Status status,
        ScheduledEvent.Status previousStatus, Channel channel) {
        this.name = name;
        this.description = description;
        this.status = status;
        this.previousStatus = previousStatus;
        this.channel = channel;
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

    public Channel getChannel() {
        return channel;
    }

}
