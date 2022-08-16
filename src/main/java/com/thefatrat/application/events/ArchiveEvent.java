package com.thefatrat.application.events;

import net.dv8tion.jda.api.entities.ThreadChannel;

public class ArchiveEvent {

    private final ThreadChannel thread;

    public ArchiveEvent(ThreadChannel thread) {this.thread = thread;}

    public ThreadChannel getThread() {
        return thread;
    }

}
