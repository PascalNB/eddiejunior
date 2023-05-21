package com.thefatrat.eddiejunior.events;

import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;

public class ArchiveEvent {

    private final ThreadChannel thread;
    private final boolean archived;

    public ArchiveEvent(ThreadChannel thread, boolean archived) {
        this.thread = thread;
        this.archived = archived;
    }

    public ThreadChannel getThreadChannel() {
        return thread;
    }

    public boolean isArchived() {
        return archived;
    }

}
