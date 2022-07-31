package com.thefatrat.application.components;

import com.thefatrat.application.PermissionChecker;
import com.thefatrat.application.sources.Server;
import com.thefatrat.application.sources.Source;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;

// TODO: user timeout
public class ModMail extends Component {

    public static final String NAME = "modmail";

    private boolean running = false;
    private MessageChannel destination;

    public ModMail(Source server) {
        super(server, NAME, false);
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    @Override
    public void register() {
        getSource().getCommandHandler().addListener(NAME, command -> {
            if (!isEnabled() || command.args().length == 0) {
                return;
            }

            switch (command.args()[0].toLowerCase()) {
                case "start" -> {
                    running = true;
                    destination = command.event().getChannel();
                    destination.sendMessageFormat("Mod mail destination set to `%s`%n" +
                            "Mod mail service started",
                        destination.getId()).queue();
                }
                case "stop" -> {
                    running = false;
                    command.event().getChannel()
                        .sendMessage("Mod mail service stopped").queue();
                }
                default -> {
                }
            }
        }, PermissionChecker.IS_ADMIN);

        ((Server) getSource()).getDirectHandler().addListener(message -> {
            if (!isEnabled() || !running || destination == null) {
                return;
            }

            String content = message.getContentRaw();
            if (content.length() < 20) {
                return;
            }

            User author = message.getAuthor();

            destination.sendMessageFormat("%s `(%s)`:%n```%s```",
                author.getAsMention(), author.getId(), content).queue();

        });
    }

}
