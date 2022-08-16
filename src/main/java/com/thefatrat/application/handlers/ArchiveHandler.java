package com.thefatrat.application.handlers;

import com.thefatrat.application.events.ArchiveEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class ArchiveHandler {

    private final Map<String, Consumer<ArchiveEvent>> map = new HashMap<>();

    public void addListener(String component, Consumer<ArchiveEvent> listener) {
        map.put(component, listener);
    }

    public void handle(ArchiveEvent archiveEvent) {
        for (Consumer<ArchiveEvent> event : map.values()) {
            event.accept(archiveEvent);
        }
    }

}
