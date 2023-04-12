package com.thefatrat.eddiejunior.components;

import com.thefatrat.eddiejunior.entities.Command;
import com.thefatrat.eddiejunior.exceptions.BotErrorException;
import com.thefatrat.eddiejunior.exceptions.BotWarningException;
import com.thefatrat.eddiejunior.reply.EditReply;
import com.thefatrat.eddiejunior.reply.Reply;
import com.thefatrat.eddiejunior.sources.Server;
import com.thefatrat.eddiejunior.util.Colors;
import com.thefatrat.eddiejunior.util.Icon;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class FanMail extends DirectComponent {

    private static final Set<String> contentTypes = Set.of(
        "image/jpeg", "image/png", "image/jpg", "image/gif", "video/mp4", "video/quicktime");

    private final Map<String, Long> timeouts = new ConcurrentHashMap<>();

    private long timeout;

    public FanMail(Server server) {
        super(server, "Fanart", "Fan art", true);

        timeout = Long.parseLong(getDatabaseManager().getSettingOrDefault("timeout", "0"));

        addSubcommands(
            new Command("timeout", "sets the timeout")
                .addOption(new OptionData(OptionType.INTEGER, "timeout", "timeout in seconds", true)
                    .setRequiredRange(0, Integer.MAX_VALUE)
                )
                .setAction((command, reply) -> {
                    long timeout = command.getArgs().get("timeout").getAsInt();
                    this.timeout = timeout;
                    reply.ok("Timout set to %d seconds", timeout);
                    getDatabaseManager().setSetting("timeout", String.valueOf(timeout));
                })
        );
    }

    @Override
    protected <T extends Reply & EditReply> void handleDirect(Message message, T reply) {
        if (!isRunning()) {
            throw new BotWarningException("The server does not accept submissions at the moment");
        }
        if (getDestination() == null) {
            throw new BotErrorException("Fan mail service not accessible");
        }
        if (getBlacklist().contains(message.getAuthor().getId())) {
            throw new BotWarningException("Cannot submit fan mail at the moment");
        }
        if (System.currentTimeMillis() - timeouts.getOrDefault(message.getAuthor().getId(), 0L) < timeout * 1000L) {
            throw new BotWarningException("You can only send a submission every %d seconds", timeout);
        }

        List<Message.Attachment> attachments = message.getAttachments();

        if (attachments.isEmpty()) {
            throw new BotWarningException("Please provide an image attachment");
        }

        Message.Attachment attachment = attachments.get(0);
        String contentType = attachment.getContentType();
        if (contentType == null) {
            throw new BotWarningException("Couldn't submit this file");
        }

        if (!contentTypes.contains(contentType)) {
            throw new BotWarningException("The following file type isn't supported: %s", contentType);
        }

        timeouts.put(message.getAuthor().getId(), System.currentTimeMillis());

        MessageEmbed embed = new EmbedBuilder()
            .setAuthor(message.getAuthor().getAsTag(), null, message.getAuthor().getEffectiveAvatarUrl())
            .setDescription(message.getAuthor().getAsMention())
            .setColor(Colors.TRANSPARENT)
            .setImage(attachment.getProxyUrl())
            .setFooter(message.getAuthor().getId())
            .build();

        getDestination().sendMessageEmbeds(embed)
            .onErrorMap(e -> null)
            .queue(m -> {
                if (m == null) {
                    reply.edit(new BotErrorException("Something went wrong, contact the moderators"));
                } else {
                    reply.edit(new EmbedBuilder()
                        .setColor(Colors.GREEN)
                        .setDescription(Icon.OK + " Image successfully submitted")
                        .build());
                }
            });
    }

    @Override
    public void start(Reply reply) {
        super.start(reply);
        reply.ok("Fan mail service started");
    }

    @Override
    public void stop(Reply reply) {
        super.stop(reply);
        reply.send(Icon.STOP, "Fan mail service stopped");
    }

}
