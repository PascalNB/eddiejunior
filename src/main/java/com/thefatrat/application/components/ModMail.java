package com.thefatrat.application.components;

import com.thefatrat.application.Command;
import com.thefatrat.application.HelpEmbedBuilder;
import com.thefatrat.application.PermissionChecker;
import com.thefatrat.application.exceptions.BotErrorException;
import com.thefatrat.application.exceptions.BotWarningException;
import com.thefatrat.application.sources.Source;
import net.dv8tion.jda.api.entities.Channel;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

// TODO: user timeout
public class ModMail extends DirectComponent {

    public static final String NAME = "Modmail";

    private final Map<String, Long> timeouts = new HashMap<>();
    private final MessageEmbed help;
    private int timeout = 0;

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
        String dest = Optional.ofNullable(getDestination())
            .map(Channel::getAsMention)
            .orElse(null);
        return String.format("""
                Enabled: %b
                Running: %b
                Destination: %s
                """,
            isEnabled(), isRunning() && !isPaused(), dest);
    }

    @Override
    public void register() {
        super.register();

        getHandler().addListener("timeout", command -> {
            if (!isEnabled() || command.args().length == 0) {
                return;
            }

            try {
                int timeout = Integer.parseInt(command.args()[0]);
                this.timeout = timeout;
                command.message().getChannel().sendMessageFormat(
                    ":white_check_mark: Timout set to %d seconds", timeout).queue();
            } catch (NumberFormatException e) {
                throw new BotErrorException("Not valid number");
            }

        }, PermissionChecker.IS_ADMIN);
    }

    @Override
    protected void handleDirect(Message message) {
        String content = message.getContentRaw();
        if (content.length() < 20) {
            throw new BotWarningException("Messages have to be at least 20 characters");
        }

        User author = message.getAuthor();
        if (System.currentTimeMillis() - timeouts.getOrDefault(author.getId(), 0L)
            >= timeout * 1000L) {

            timeouts.put(author.getId(), System.currentTimeMillis());
            getDestination().sendMessageFormat(":email: %s `(%s)`:%n```%s```",
                author.getAsMention(), author.getId(), content).queue();
            message.getChannel().sendMessage("" +
                ":white_check_mark: Message successfully submitted").queue();
        } else {
            throw new BotWarningException(
                String.format("You can only send a message every %d seconds", timeout));
        }
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
