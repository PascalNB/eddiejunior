package com.thefatrat.application;

import com.thefatrat.application.handlers.CommandHandler;
import com.thefatrat.application.handlers.EventHandler;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Bot extends ListenerAdapter {

    public static final String DEFAULT_PREFIX = "-";
    private static final Map<String, String> prefixes = new HashMap<>();

    private final EventHandler<Command> commandHandler = new CommandHandler();

    public static void setPrefix(String guild, String prefix) {
        prefixes.put(guild, prefix);
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (event.getAuthor().isBot() || event.getAuthor().isSystem()) {
            return;
        }
        Message message = event.getMessage();
        String content = message.getContentRaw();

        String prefix;
        if (event.isFromGuild()) {
            String guildId = event.getGuild().getId();
            System.out.println(guildId);
            prefixes.putIfAbsent(guildId, DEFAULT_PREFIX);
            prefix = prefixes.get(guildId);
        } else {
            prefix = DEFAULT_PREFIX;
        }

        if (content.startsWith(prefix)) {
            String[] split = content.split("\\s");
            String command = split[0].substring(prefix.length()).toLowerCase();
            String[] args = split.length > 1
                ? Arrays.copyOfRange(split, 1, split.length)
                : new String[0];
            commandHandler.handle(new Command(command, args, message));
        }
    }

}
