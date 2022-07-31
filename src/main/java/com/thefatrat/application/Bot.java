package com.thefatrat.application;

import com.thefatrat.application.components.Component;
import com.thefatrat.application.sources.Direct;
import com.thefatrat.application.sources.Server;
import com.thefatrat.application.sources.Source;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class Bot extends ListenerAdapter {

    public static Bot instance;

    public static final String DEFAULT_PREFIX = "-";

    private final Map<String, Source> sources = new HashMap<>();

    private Class<? extends Component>[] components;

    private Bot() {
        Source direct = new Direct();
        sources.put(null, direct);
    }

    public static Bot getInstance() {
        if (instance == null) {
            instance = new Bot();
        }
        return instance;
    }

    public Server getServer(String id) {
        return (Server) sources.get(id);
    }

    @SafeVarargs
    public final void setComponents(Class<? extends Component>... components) {
        this.components = components;
    }

    @Override
    public void onGuildReady(@NotNull GuildReadyEvent event) {
        String id = event.getGuild().getId();
        Server server = new Server(id);
        sources.put(server.getId(), server);
        server.registerComponents(components);
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (event.getAuthor().isBot() || event.getAuthor().isSystem()) {
            return;
        }
        Message message = event.getMessage();
        if (message.isFromGuild()) {
            String id = message.getGuild().getId();
            sources.get(id).receiveMessage(message);
        } else {
            sources.get(null).receiveMessage(message);
        }
    }

}
