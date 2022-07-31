package com.thefatrat.application.components;

import com.thefatrat.application.Command;
import com.thefatrat.application.sources.Source;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;

// TODO: user timeout
public class ModMail extends DirectComponent {

    public static final String NAME = "Modmail";

    public ModMail(Source server) {
        super(server, NAME);
    }

    @Override
    public String getHelp() {
        return "";
    }

    @Override
    protected void handleDirect(Message message) {
        String content = message.getContentRaw();
        if (content.length() < 20) {
            return;
        }

        User author = message.getAuthor();

        getDestination().sendMessageFormat(":email: %s `(%s)`:%n```%s```",
            author.getAsMention(), author.getId(), content).queue();
    }

    @Override
    protected void stop(Command command) {
        getDestination().sendMessageFormat(
            ":stop_sign: Mod mail service stopped",
            getDestination().getId()
        ).queue();
    }

    @Override
    protected void start(Command command) {
        getDestination().sendMessageFormat(
            ":white_check_mark: Mod mail service started",
            getDestination().getId()
        ).queue();
    }

}
