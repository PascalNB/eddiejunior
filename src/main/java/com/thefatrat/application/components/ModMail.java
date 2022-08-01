package com.thefatrat.application.components;

import com.thefatrat.application.Command;
import com.thefatrat.application.HelpEmbedBuilder;
import com.thefatrat.application.exceptions.BotWarningException;
import com.thefatrat.application.sources.Source;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;

// TODO: user timeout
public class ModMail extends DirectComponent {

    public static final String NAME = "Modmail";

    private final MessageEmbed help;

    public ModMail(Source server) {
        super(server, NAME);
        // TODO
        help = new HelpEmbedBuilder(NAME)
            .addCommand("modmail start", "")
            .addCommand("modmail start [channel]", "")
            .addCommand("modmail stop", "")
            .addCommand("modmail destination", "")
            .addCommand("modmail destination [channel]", "")
            .build(getColor());
    }

    @Override
    public int getColor() {
        return 0xd6943e;
    }

    @Override
    public MessageEmbed getHelp() {
        return help;
    }

    @Override
    public String getStatus() {
        return String.format("""
                Enabled: %b
                Running: %b
                Destination: %s
                """,
            isEnabled(), isRunning() && !isPaused(), getDestination().getAsMention());
    }

    @Override
    protected void handleDirect(Message message) {
        String content = message.getContentRaw();
        if (content.length() < 20) {
            throw new BotWarningException("Messages have to be at least 20 characters");
        }

        User author = message.getAuthor();

        getDestination().sendMessageFormat(":email: %s `(%s)`:%n```%s```",
            author.getAsMention(), author.getId(), content).queue();
    }

    @Override
    protected void stop(Command command) {
        command.message().getChannel().sendMessageFormat(
            ":stop_sign: Mod mail service stopped",
            getDestination().getId()
        ).queue();
    }

    @Override
    protected void start(Command command) {
        command.message().getChannel().sendMessageFormat(
            ":white_check_mark: Mod mail service started",
            getDestination().getId()
        ).queue();
    }

}
