package com.thefatrat.application.handlers;

import com.thefatrat.application.events.ArchiveEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class ArchiveHandler {

    private final Set<Consumer<ArchiveEvent>> set = new HashSet<>();

    public void addListener(Consumer<ArchiveEvent> listener) {
        set.add(listener);
    }

    public void handle(ArchiveEvent archiveEvent) {
        for (Consumer<ArchiveEvent> event : set) {
            event.accept(archiveEvent);
        }
    }

}
