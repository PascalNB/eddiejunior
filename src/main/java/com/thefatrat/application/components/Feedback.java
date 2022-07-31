package com.thefatrat.application.components;

import com.thefatrat.application.PermissionChecker;
import com.thefatrat.application.sources.Server;
import com.thefatrat.application.sources.Source;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Feedback extends Component {

    public static final String NAME = "feedback";
    private static final String URL_REGEX = "^(https?|ftp|file)://[-a-zA-Z\\d+&@#/%?=~_|!:,.;]*" +
        "[-a-zA-Z\\d+&@#/%=~_|]";

    private boolean running = false;
    private MessageChannel destination;
    private final Set<String> users = new HashSet<>();

    public Feedback(Source server) {
        super(server, NAME, false);
    }

    public void setDestination(MessageChannel destination) {
        this.destination = destination;
    }

    @Override
    public void register() {
        getSource().getCommandHandler().addListener(NAME, command -> {
            if (!isEnabled() || command.args().length == 0) {
                return;
            }

            switch (command.args()[0].toLowerCase()) {
                case "start" -> {
                    ModMail modMail = ((ModMail) getSource().getComponent(ModMail.NAME));
                    modMail.setRunning(false);
                    users.clear();

                    String message = "";
                    MessageChannel newDestination = command.event().getChannel();
                    if (destination == null ||
                        !newDestination.getId().equals(destination.getId())) {

                        destination = newDestination;
                        message += String.format(":gear: Feedback destination set to `%s`%n",
                            destination.getId());
                    }

                    message += (modMail.isEnabled()
                        ? ":white_check_mark: Feedback session started and mod mail disabled"
                        : ":white_check_mark: Feedback session started");
                    command.event().getChannel().sendMessage(message).queue();
                    running = true;
                }
                case "stop" -> {
                    running = false;
                    ModMail modMail = ((ModMail) getSource().getComponent(ModMail.NAME));
                    modMail.setRunning(true);

                    String message = modMail.isEnabled()
                        ? ":stop_sign: Feedback session stopped and mod mail enabled"
                        : ":stop_sign: Feedback session stopped";
                    command.event().getChannel().sendMessage(message).queue();
                }
                case "reset" -> {
                    if (command.args().length == 1) {
                        users.clear();
                        command.event().getChannel()
                            .sendMessage(":arrows_counterclockwise: Feedback session reset, users" +
                                " can submit again")
                            .queue();
                        return;
                    }
                    for (User user : command.event().getMentions().getUsers()) {
                        users.remove(user.getId());
                    }
                    for (int i = 1, length = command.args().length; i < length; i++) {
                        users.remove(command.args()[i]);
                    }
                    command.event().getChannel()
                        .sendMessage(":arrows_counterclockwise: Feedback session reset for " +
                            "the given users, they can submit again")
                        .queue();
                }
                default -> {
                }
            }
        }, PermissionChecker.IS_ADMIN);

        ((Server) getSource()).getDirectHandler().addListener(message -> {
            if (!isEnabled() || !running || destination == null) {
                return;
            }

            List<Message.Attachment> attachments = message.getAttachments();

            String url = null;
            if (attachments.size() > 0) {
                url = attachments.get(0).getUrl();

            } else {
                String[] content = message.getContentRaw().split("\\s+");

                for (String part : content) {
                    if (isUrl(part)) {
                        url = part;
                        break;
                    }
                }
            }
            if (url == null) {
                return;
            }

            User author = message.getAuthor();

            if (users.contains(author.getId())) {
                message.getChannel().sendMessage(":warning: You can only submit once!").queue();
                return;
            }

            users.add(author.getId());
            destination.sendMessageFormat("%s `(%s)`:%n<%s>",
                author.getAsMention(), author.getId(), url).queue();
            message.getChannel()
                .sendMessage(":white_check_mark: Successfully submitted!")
                .queue();
        });
    }

    private boolean isUrl(String message) {
        return message.matches(URL_REGEX);
    }

}
