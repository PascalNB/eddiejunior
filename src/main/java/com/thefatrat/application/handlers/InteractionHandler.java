package com.thefatrat.application.handlers;

import com.thefatrat.application.exceptions.BotErrorException;
import com.thefatrat.application.exceptions.BotException;
import com.thefatrat.application.util.InteractionEvent;
import com.thefatrat.application.util.Reply;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public class InteractionHandler implements Handler<InteractionEvent> {

    private final Map<String, BiConsumer<InteractionEvent, Reply>> map = new HashMap<>();

    public void addListener(String component, BiConsumer<InteractionEvent, Reply> listener) {
        map.put(component, listener);
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
        BiConsumer<InteractionEvent, Reply> consumer =
            map.get(embed.getFooter().getText().toLowerCase());
        if (consumer == null) {
            throw new BotErrorException("Could not perform action");
        }
        consumer.accept(event, reply);
    }

}
