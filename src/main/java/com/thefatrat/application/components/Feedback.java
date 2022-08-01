package com.thefatrat.application.components;

import com.thefatrat.application.Command;
import com.thefatrat.application.HelpEmbedBuilder;
import com.thefatrat.application.exceptions.BotWarningException;
import com.thefatrat.application.sources.Source;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Feedback extends DirectComponent {

    public static final String NAME = "Feedback";
    private static final String URL_REGEX = "^(https?|ftp|file)://[-a-zA-Z\\d+&@#/%?=~_|!:,.;]*" +
        "[-a-zA-Z\\d+&@#/%=~_|]";

    private final Set<String> users = new HashSet<>();
    private final MessageEmbed help;

    public Feedback(Source server) {
        super(server, NAME);
        help = new HelpEmbedBuilder(NAME)
            .addCommand("feedback start", "start the feedback session")
            .addCommand("feedback start [channel]",
                "start the feedback session in the given channel")
            .addCommand("feedback stop", "stop the current feedback session")
            .addCommand("feedback destination",
                "set the destination channel to the current channel")
            .addCommand("feedback destination [channel]",
                "set the destination channel to the given channel")
            .addCommand("feedback reset", "allow submissions for all users again")
            .addCommand("feedback reset [users]", "allow specific users to submit again")
            .build();
    }

    @Override
    public MessageEmbed getHelp() {
        return help;
    }

    @Override
    public void register() {
        super.register();

        getHandler().addListener("reset", command -> {
            if (command.args().length == 0) {
                users.clear();
                command.message().getChannel().sendMessage(
                    ":arrows_counterclockwise: Feedback session reset, users can submit again"
                ).queue();
                return;
            }

            for (User user : command.message().getMentions().getUsers()) {
                users.remove(user.getId());
            }
            for (int i = 0, length = command.args().length; i < length; i++) {
                users.remove(command.args()[i]);
            }

            command.message().getChannel().sendMessage(
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
            throw new BotWarningException("Please send a valid file or link");
        }

        User author = message.getAuthor();

        if (users.contains(author.getId())) {
            throw new BotWarningException("You can only submit once");
        }

        users.add(author.getId());
        getDestination().sendMessageFormat("%s `(%s)`:%n<%s>",
            author.getAsMention(), author.getId(), url).queue();
        message.getChannel()
            .sendMessage(":white_check_mark: Successfully submitted")
            .queue();
    }

    @Override
    protected void start(Command command) {
        users.clear();
        command.message().getChannel().sendMessage(
            ":white_check_mark: Feedback session started"
        ).queue();
    }

    @Override
    protected void stop(Command command) {
        command.message().getChannel().sendMessage(
            ":stop_sign: Feedback session stopped"
        ).queue();
    }

    private boolean isUrl(String message) {
        return message.matches(URL_REGEX);
    }

}
