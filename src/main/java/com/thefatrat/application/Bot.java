package com.thefatrat.application;

import com.thefatrat.application.handlers.CommandHandler;
import com.thefatrat.application.handlers.ListenerHandler;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class Bot extends ListenerAdapter {

    private static String prefix = "#";

    private final ListenerHandler<Command> commandHandler = new CommandHandler();

    public Bot() {

    }

    public static void setPrefix(String prefix) {
        Bot.prefix = prefix;
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (event.getAuthor().isBot() || event.getAuthor().isSystem()) {
            return;
        }
        Message message = event.getMessage();
        String content = message.getContentRaw();
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
