package com.thefatrat.eddiejunior.sources;

import com.thefatrat.eddiejunior.Bot;
import com.thefatrat.eddiejunior.HandlerCollection;
import com.thefatrat.eddiejunior.events.ButtonEvent;
import com.thefatrat.eddiejunior.events.SelectEvent;
import com.thefatrat.eddiejunior.exceptions.BotErrorException;
import com.thefatrat.eddiejunior.exceptions.BotWarningException;
import com.thefatrat.eddiejunior.handlers.MapHandler;
import com.thefatrat.eddiejunior.reply.MenuReply;
import com.thefatrat.eddiejunior.reply.Reply;
import com.thefatrat.eddiejunior.util.Colors;
import com.thefatrat.eddiejunior.util.Icon;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditData;
import org.jetbrains.annotations.NotNull;

import javax.annotation.CheckReturnValue;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Direct {

    private final Map<String, Message> cache = new HashMap<>();
    private final HandlerCollection<User> handlerCollection = new HandlerCollection<>();

    public Direct() {
        getStringSelectHandler().addListener("component", (event, reply) -> {
            String[] split = event.getOption().getValue().split("-");
            String serverId = split[0];
            String component = split[1];

            Message userMessage = cache.remove(event.getUser().getId());

            Server server = Bot.getInstance().getServer(serverId);
            if (server == null) {
                throw new BotErrorException("Something went wrong");
            }

            if (!server.getDirectMessageHandler().getComponents().contains(component)) {
                throw new BotErrorException("Could not send to the given service, try again");
            }

            server.getDirectMessageHandler().handle(component, userMessage, reply);
        });
        getStringSelectHandler().addListener("server", (event, reply) ->
            reply.edit(MessageEditData.fromCreateData(
                getComponentMenu(event.getUser().getId(), event.getOption().getValue())
            ))
        );

        getButtonHandler().addListener("x", (event, reply) -> {
            cache.remove(event.getActor().getId());
            reply.edit(Icon.STOP, "Successfully cancelled");
        });
    }

    public MapHandler<SelectEvent<SelectOption>, MenuReply> getStringSelectHandler() {
        return handlerCollection.getStringSelectHandler();
    }

    public MapHandler<ButtonEvent<User>, MenuReply> getButtonHandler() {
        return handlerCollection.getButtonMapHandler();
    }

    public void receiveMessage(@NotNull Message message, Reply reply) {
        String userId = message.getAuthor().getId();
        message.getChannel().sendTyping().queue();
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

    @NotNull
    @CheckReturnValue
    private MessageCreateData getComponentMenu(String userId, String serverId) {
        Server server = Bot.getInstance().getServer(serverId);
        if (server == null) {
            throw new BotErrorException("Something went wrong");
        }

        Set<Map.Entry<String, String>> names = server.getDirectMessageHandler().getNames();

        if (names.isEmpty()) {
            cache.remove(userId);
            throw new BotWarningException("The server does not handle any messages at the moment");
        }

        StringSelectMenu.Builder menu = StringSelectMenu.create("component").setMaxValues(1);

        for (Map.Entry<String, String> name : names) {
            menu.addOption(name.getValue(), serverId + "-" + name.getKey());
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
    public MessageCreateData getServerMenu(@NotNull List<Guild> guilds) {
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
