package com.thefatrat.application.sources;

import com.thefatrat.application.Bot;
import com.thefatrat.application.Command;
import com.thefatrat.application.handlers.MessageHandler;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;

import java.util.Arrays;
import java.util.Objects;

public class Server extends Source {

    private final String id;
    private final MessageHandler directHandler = new MessageHandler();

    private String prefix = Bot.DEFAULT_PREFIX;

    public Server(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public MessageHandler getDirectHandler() {
        return directHandler;
    }

    @Override
    public void receiveMessage(Message message) {
        String content = message.getContentRaw();

        if (content.startsWith(prefix)) {
            Member member = Objects.requireNonNull(
                message.getGuild().getMember(message.getAuthor()));

            String[] split = content.split("\\s+");
            String command = split[0].substring(prefix.length()).toLowerCase();
            String[] args = split.length > 1
                ? Arrays.copyOfRange(split, 1, split.length)
                : new String[0];
            getCommandHandler().handle(new Command(command, args, message, member));
        } else {
            getMessageHandler().handle(message);
        }
    }

}
