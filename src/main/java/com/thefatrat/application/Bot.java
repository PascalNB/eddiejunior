package com.thefatrat.application;

import com.thefatrat.application.components.Component;
import com.thefatrat.application.exceptions.BotException;
import com.thefatrat.application.sources.Direct;
import com.thefatrat.application.sources.Server;
import com.thefatrat.application.sources.Source;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class Bot extends ListenerAdapter {

    public static final String DEFAULT_PREFIX = "-";
    public static Bot instance;

    private final Map<String, Source> sources = new HashMap<>();
    private Class<? extends Component>[] components;
    private long time = 0;

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

    private void loadServer(String id) {
        Server server = new Server(id);
        sources.put(server.getId(), server);
        server.registerComponents(components);
    }

    public String getUptime() {
        long t = System.currentTimeMillis() - time;
        long hours = TimeUnit.MILLISECONDS.toHours(t);
        long min = TimeUnit.MILLISECONDS.toMinutes(t);
        long sec = TimeUnit.MILLISECONDS.toSeconds(t);
        return String.format("%d hours, %d minutes, %d seconds",
            hours,
            min - TimeUnit.HOURS.toMinutes(hours),
            sec - TimeUnit.MINUTES.toSeconds(min)
        );
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        time = System.currentTimeMillis();
    }

    @Override
    public void onGuildReady(@NotNull GuildReadyEvent event) {
        String id = event.getGuild().getId();
        loadServer(id);
    }

    @Override
    public void onGuildJoin(@NotNull GuildJoinEvent event) {
        String id = event.getGuild().getId();
        loadServer(id);
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (event.getAuthor().isBot() || event.getAuthor().isSystem()) {
            return;
        }
        Message message = event.getMessage();
        try {
            if (message.isFromGuild()) {
                String id = message.getGuild().getId();
                sources.get(id).receiveMessage(message);
            } else {
                sources.get(null).receiveMessage(message);
            }
        } catch (BotException e) {
            event.getChannel().sendMessage(e.getMessage()).queue();
        }
    }

}
