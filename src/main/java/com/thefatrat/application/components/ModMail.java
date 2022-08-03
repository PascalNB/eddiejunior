package com.thefatrat.application.components;

import com.thefatrat.application.exceptions.BotErrorException;
import com.thefatrat.application.exceptions.BotWarningException;
import com.thefatrat.application.sources.Server;
import com.thefatrat.application.util.Command;
import com.thefatrat.application.util.CommandEvent;
import com.thefatrat.application.util.Reply;
import net.dv8tion.jda.api.entities.Channel;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

// TODO: user timeout
public class ModMail extends DirectComponent {

    public static final String NAME = "Modmail";

    private final Map<String, Long> timeouts = new HashMap<>();
    private long timeout = 0;

    public ModMail(Server server) {
        super(server, NAME);

        addSubcommands(new Command("timeout", "sets the timeout")
            .addOption(new OptionData(OptionType.INTEGER, "timeout", "timeout in ms", true))
            .setAction((command, reply) -> {
                long timeout = command.getArgs().get("timeout").getAsInt();
                if (timeout < 0) {
                    throw new BotErrorException("The timeout should be 0 or larger");
                }
                this.timeout = timeout;
                reply.sendMessageFormat(
                    ":white_check_mark: Timout set to %d seconds", timeout);
            })
        );
    }

    @Override
    public int getColor() {
        return 0xd6943e;
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
    protected void handleDirect(Message message, Reply reply) {
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
    protected void stop(CommandEvent command, Reply reply) {
        super.stop(command, reply);
        reply.sendMessageFormat(
            ":stop_sign: Mod mail service stopped",
            getDestination().getId()
        );
    }

    @Override
    protected void start(CommandEvent command, Reply reply) {
        super.start(command, reply);
        reply.sendMessageFormat(
            ":white_check_mark: Mod mail service started",
            getDestination().getId()
        );
    }

}
