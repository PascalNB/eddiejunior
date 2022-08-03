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

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class Feedback extends DirectComponent {

    public static final String NAME = "Feedback";
    private static final String URL_REGEX = "^(https?|ftp|file)://[-a-zA-Z\\d+&@#/%?=~_|!:,.;]*" +
        "[-a-zA-Z\\d+&@#/%=~_|]";

    private final Set<String> users = new HashSet<>();
    private int submissions = 0;

    public Feedback(Server server) {
        super(server, NAME);
    }

    @Override
    public int getColor() {
        return 0x308acb;
    }

    @Override
    public void register() {
        super.register();

        addSubcommands(new Command("reset", "allow submissions for users again")
            .addOption(new OptionData(OptionType.STRING, "member", "member id"))
            .setAction((command, reply) -> {
                if (!command.getArgs().containsKey("member")) {
                    users.clear();
                    reply.sendMessage(
                        ":arrows_counterclockwise: Feedback session reset, users can submit again"
                    );
                    return;
                }

                long id;
                try {
                    id = command.getArgs().get("member").getAsLong();
                } catch (NumberFormatException e) {
                    throw new BotErrorException("Not a valid member id");
                }

                command.getGuild().retrieveMemberById(id)
                    .onErrorMap(e -> null)
                    .queue(member -> {
                        if (member == null) {
                            reply.sendMessage(new BotErrorException(
                                "The given member was not found").getMessage());
                            return;
                        }

                        users.remove(member.getId());

                        reply.sendMessageFormat(":arrows_counterclockwise: Feedback session " +
                            "reset for %s, they can submit again", member.getAsMention());
                    });
            })
        );
    }

    @Override
    public String getStatus() {
        String dest = Optional.ofNullable(getDestination())
            .map(Channel::getAsMention)
            .orElse(null);
        return String.format("""
                Enabled: %b
                Running: %b
                Submissions: %d
                Destination: %s
                """,
            isEnabled(), isRunning() && !isPaused(), submissions, dest);
    }

    @Override
    protected void handleDirect(Message message, Reply reply) {
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
        submissions++;
        reply.sendMessage(":white_check_mark: Successfully submitted");
    }

    @Override
    protected void start(CommandEvent command, Reply reply) {
        super.start(command, reply);
        users.clear();
        submissions = 0;
        reply.sendMessage(
            ":white_check_mark: Feedback session started"
        );
    }

    @Override
    protected void stop(CommandEvent command, Reply reply) {
        super.stop(command, reply);
        submissions = 0;
        reply.sendMessage(
            ":stop_sign: Feedback session stopped"
        );
    }

    private boolean isUrl(String message) {
        return message.matches(URL_REGEX);
    }

}
