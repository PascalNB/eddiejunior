package com.thefatrat.application.handlers;

import com.thefatrat.application.entities.Reply;
import com.thefatrat.application.events.InteractionEvent;
import com.thefatrat.application.exceptions.BotErrorException;
import com.thefatrat.application.exceptions.BotException;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public class InteractionHandler implements Handler<InteractionEvent> {

    private final Map<String, Map<String, BiConsumer<InteractionEvent, Reply>>> map = new HashMap<>();

    public void addListener(String component, String key, BiConsumer<InteractionEvent, Reply> listener) {
        Map<String, BiConsumer<InteractionEvent, Reply>> interactions = map.getOrDefault(component, new HashMap<>());
        interactions.put(key, listener);
        map.put(component, interactions);
    }

    @Override
    public void handle(InteractionEvent event, Reply reply) throws BotException {
        List<MessageEmbed> embeds = event.getMessage().getEmbeds();
        if (embeds.isEmpty()) {
            throw new BotErrorException("Message does not contain embeds");
        }
        MessageEmbed embed = embeds.get(0);
        if (embed.getFooter() == null || embed.getFooter().getText() == null) {
            throw new BotErrorException("Could not perform action");
        }
        Map<String, BiConsumer<InteractionEvent, Reply>> interactions =
            map.get(embed.getFooter().getText().toLowerCase());
        if (interactions == null || !interactions.containsKey(event.getAction())) {
            throw new BotErrorException("Could not perform action");
        }
        interactions.get(event.getAction()).accept(event, reply);
    }

}
