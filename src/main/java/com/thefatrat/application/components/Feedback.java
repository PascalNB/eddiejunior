package com.thefatrat.application.components;

import com.thefatrat.application.Command;
import com.thefatrat.application.sources.Source;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Feedback extends DirectComponent {

    public static final String NAME = "Feedback";
    private static final String URL_REGEX = "^(https?|ftp|file)://[-a-zA-Z\\d+&@#/%?=~_|!:,.;]*" +
        "[-a-zA-Z\\d+&@#/%=~_|]";

    private final Set<String> users = new HashSet<>();

    public Feedback(Source server) {
        super(server, NAME);
    }

    @Override
    public String getHelp() {
        return """
            `feedback start`
              - start the feedback session.
            `feedback start [channel]`
              - start the feedback session in the given channel.
            `feedback stop`
              - stop the current feedback session.
            `feedback destination`
              - set the destination channel to the current channel.
            `feedback destination [channel]`
              - set the destination channel to the given channel.
            `feedback reset`
              - allow submissions for all users again.
            `feedback reset [users]`
              - allow specific users to submit again.
            """;
    }

    @Override
    public void register() {
        super.register();

        getHandler().addListener("reset", command -> {
            if (command.args().length == 0) {
                users.clear();
                command.event().getChannel().sendMessage(
                    ":arrows_counterclockwise: Feedback session reset, users can submit again"
                ).queue();
                return;
            }

            for (User user : command.event().getMentions().getUsers()) {
                users.remove(user.getId());
            }
            for (int i = 0, length = command.args().length; i < length; i++) {
                users.remove(command.args()[i]);
            }

            command.event().getChannel().sendMessage(
                ":arrows_counterclockwise: Feedback session reset for " +
                    "the given users, they can submit again"
            ).queue();
        });
    }

    @Override
    protected void handleDirect(Message message) {
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
            message.getChannel().sendMessage(
                ":warning: You can only submit once!"
            ).queue();
            return;
        }

        users.add(author.getId());
        getDestination().sendMessageFormat("%s `(%s)`:%n<%s>",
            author.getAsMention(), author.getId(), url).queue();
        message.getChannel()
            .sendMessage(":white_check_mark: Successfully submitted!")
            .queue();
    }

    @Override
    protected void start(Command command) {
        users.clear();
        command.event().getChannel().sendMessage(
            ":white_check_mark: Feedback session started"
        ).queue();
    }

    @Override
    protected void stop(Command command) {
        command.event().getChannel().sendMessage(
            ":stop_sign: Feedback session stopped"
        ).queue();
    }

    private boolean isUrl(String message) {
        return message.matches(URL_REGEX);
    }

}
