package com.thefatrat.application.sources;

import com.thefatrat.application.Bot;
import com.thefatrat.application.events.ButtonEvent;
import com.thefatrat.application.events.StringSelectEvent;
import com.thefatrat.application.exceptions.BotErrorException;
import com.thefatrat.application.exceptions.BotWarningException;
import com.thefatrat.application.handlers.Handler;
import com.thefatrat.application.handlers.MapHandler;
import com.thefatrat.application.handlers.SetHandler;
import com.thefatrat.application.reply.ComponentReply;
import com.thefatrat.application.reply.Reply;
import com.thefatrat.application.util.Colors;
import com.thefatrat.application.util.Icon;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

import javax.annotation.CheckReturnValue;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Direct {

    private final Map<String, Message> cache = new HashMap<>();
    private final SetHandler<StringSelectEvent, ComponentReply> stringSelectHandler = new SetHandler<>();
    private final SetHandler<ButtonEvent<User>, ComponentReply> buttonHandler = new SetHandler<>();

    public Direct() {
        stringSelectHandler.addListener((event, reply) -> {
            if ("component".equals(event.getMenuId())) {
                String[] split = event.getOption().split("-");
                String serverId = split[0];
                String component = split[1];

                Message userMessage = cache.remove(event.getUser().getId());

                Server server = Bot.getInstance().getServer(serverId);
                if (server == null) {
                    throw new BotErrorException("Something went wrong");
                }

                MapHandler<Message, Reply> handler = server.getDirectHandler();

                if (!handler.getKeys().contains(component)) {
                    throw new BotErrorException("Could not send to the given service, try again");
                }

                handler.handleOne(component, userMessage, reply.getEditor());

            } else if ("server".equals(event.getMenuId())) {
                MessageCreateData data = getComponentMenu(event.getUser().getId(), event.getOption());
                reply.getEditor().send(data);
            }
        });

        buttonHandler.addListener((event, reply) -> {
            if ("x".equals(event.getButtonId())) {
                cache.remove(event.getUser().getId());
                reply.getEditor().send(Icon.STOP, "Successfully cancelled");
            }
        });
    }

    public Handler<StringSelectEvent, ComponentReply> getStringSelectHandler() {
        return stringSelectHandler;
    }

    public Handler<ButtonEvent<User>, ComponentReply> getButtonHandler() {
        return buttonHandler;
    }

    public void receiveMessage(Message message, Reply reply) {
        String userId = message.getAuthor().getId();
        List<Guild> mutualGuilds = Bot.getInstance().retrieveMutualGuilds(message.getAuthor()).complete();

        if (mutualGuilds.isEmpty() || cache.containsKey(userId)) {
            return;
        }

        cache.put(message.getAuthor().getId(), message);

        MessageCreateData data;
        if (mutualGuilds.size() == 1) {
            String serverId = mutualGuilds.get(0).getId();
            data = getComponentMenu(userId, serverId);
        } else {
            data = getServerMenu(mutualGuilds);
        }

        reply.send(data);
    }

    @CheckReturnValue
    private MessageCreateData getComponentMenu(String userId, String serverId) {
        Server server = Bot.getInstance().getServer(serverId);
        if (server == null) {
            throw new BotErrorException("Something went wrong");
        }

        Set<String> keys = server.getDirectHandler().getKeys();

        if (keys.isEmpty()) {
            cache.remove(userId);
            throw new BotWarningException("The server does not handle any messages at the moment");
        }

        StringSelectMenu.Builder menu = StringSelectMenu.create("component")
            .setMaxValues(1);
        for (String key : server.getDirectHandler().getKeys()) {
            String name = key.substring(0, 1).toUpperCase() + key.substring(1);
            menu.addOption(name, serverId + "-" + key);
        }

        return new MessageCreateBuilder()
            .addEmbeds(new EmbedBuilder()
                .setColor(Colors.TRANSPARENT)
                .setTitle("What service do you want to send this to?")
                .build()
            )
            .setComponents(
                ActionRow.of(menu.build()),
                ActionRow.of(Button.danger("x", "Cancel").withEmoji(Emoji.fromUnicode("✖")))
            )
            .build();
    }

    @CheckReturnValue
    public MessageCreateData getServerMenu(List<Guild> guilds) {
        StringSelectMenu.Builder menu = StringSelectMenu.create("server").setRequiredRange(1, 1);

        for (Guild guild : guilds) {
            menu.addOption(guild.getName(), guild.getId());
        }

        return new MessageCreateBuilder()
            .addEmbeds(new EmbedBuilder()
                .setColor(Colors.TRANSPARENT)
                .setTitle("What server do you want to send this to?")
                .build()
            )
            .setComponents(
                ActionRow.of(menu.build()),
                ActionRow.of(Button.danger("x", "Cancel").withEmoji(Emoji.fromUnicode("✖")))
            )
            .build();
    }

}
