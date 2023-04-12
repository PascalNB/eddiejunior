package com.thefatrat.eddiejunior.components;

import com.thefatrat.eddiejunior.entities.Command;
import com.thefatrat.eddiejunior.exceptions.BotErrorException;
import com.thefatrat.eddiejunior.exceptions.BotWarningException;
import com.thefatrat.eddiejunior.reply.EditReply;
import com.thefatrat.eddiejunior.reply.Reply;
import com.thefatrat.eddiejunior.sources.Server;
import com.thefatrat.eddiejunior.util.Colors;
import com.thefatrat.eddiejunior.util.Icon;
import com.thefatrat.eddiejunior.util.PermissionChecker;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.IMentionable;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditData;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FanMail extends DirectComponent {

    private static final Set<String> contentTypes = Set.of(
        "image/jpeg", "image/png", "image/jpg", "image/gif", "video/mp4", "video/quicktime");

    private final Map<String, Long> timeouts = new ConcurrentHashMap<>();

    private String submissionChannelId;
    private long timeout;

    public FanMail(Server server) {
        super(server, "Fanart", "Fanart Submissions", true);

        timeout = Long.parseLong(getDatabaseManager().getSettingOrDefault("timeout", "0"));
        submissionChannelId = getDatabaseManager().getSetting("submissionchannel");

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
                    getServer().log(command.getMember().getUser(), "Set fanart timeout to `%d` seconds", timeout);
                }),

            new Command("submissionchannel", "sets the channel were submissions will be posted to for review")
                .addOption(new OptionData(OptionType.CHANNEL, "channel", "channel", true)
                    .setChannelTypes(ChannelType.TEXT)
                )
                .setAction((command, reply) -> {
                    TextChannel channel = command.getArgs().get("channel").getAsChannel().asTextChannel();
                    submissionChannelId = channel.getId();

                    PermissionChecker.requireSend(channel);

                    reply.ok("Set submission review channel to %s", channel.getAsMention());
                    getDatabaseManager().setSetting("submissionchannel", channel.getId());
                    getServer().log(command.getMember().getUser(), "Set fanart submission review channel to %s " +
                        "(`%s`)", channel.getAsMention(), channel.getId());
                })
        );

        getServer().getButtonHandler().addListener((event, reply) -> {
            if (event.getButtonId().equals("fanart_approve")) {
                MessageCreateData data = MessageCreateBuilder.fromMessage(event.getMessage())
                    .setComponents()
                    .build();

                TextChannel destination = getDestination();

                if (destination == null || !destination.canTalk()) {
                    throw new BotErrorException("Destination channel not accessible");
                }

                Message response = destination.sendMessage(data)
                    .onErrorMap(e -> null)
                    .complete();

                if (response == null) {
                    throw new BotErrorException("Couldn't approve submission");
                }

                MessageEmbed embed = event.getMessage().getEmbeds().get(0);

                event.getMessage().editMessage(MessageEditData.fromCreateData(data)).queue(
                    message -> message.addReaction(Emoji.fromUnicode("❤️")).queue()
                );
                reply.hide();
                reply.ok("Submission approved\n%s", response.getJumpUrl());
                getServer().log(event.getUser().getUser(), "Approved fanart submission by %s (`%`)\n%s",
                    embed.getDescription(), Objects.requireNonNull(embed.getFooter()).getText(), response.getJumpUrl());

            } else if (event.getButtonId().equals("fanart_deny")) {
                Message message = event.getMessage();
                message.editMessageComponents().queue();

                MessageEmbed embed = message.getEmbeds().get(0);

                reply.hide();
                reply.send(Icon.STOP, "Denied fanart submission");
                getServer().log(event.getUser().getUser(), "Denied fanart submission by %s (`%s`)",
                    embed.getDescription(), Objects.requireNonNull(embed.getFooter()).getText());
            }
        });
    }

    @Override
    protected <T extends Reply & EditReply> void handleDirect(Message message, T reply) {
        if (!isRunning() || submissionChannelId == null) {
            throw new BotErrorException("Fanart service not accessible");
        }
        if (getBlacklist().contains(message.getAuthor().getId())) {
            throw new BotWarningException("Cannot submit fanart at the moment");
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

        MessageCreateData data = new MessageCreateBuilder()
            .addEmbeds(new EmbedBuilder()
                .setAuthor(message.getAuthor().getAsTag(), null, message.getAuthor().getEffectiveAvatarUrl())
                .setDescription(message.getAuthor().getAsMention())
                .setColor(Colors.TRANSPARENT)
                .setImage(attachment.getProxyUrl())
                .setFooter(message.getAuthor().getId())
                .build())
            .addActionRow(
                Button.success("fanart_approve", "Approve").withEmoji(Emoji.fromUnicode("✔️")),
                Button.danger("fanart_deny", "Deny").withEmoji(Emoji.fromUnicode("✖️"))
            )
            .build();

        TextChannel output = getServer().getGuild().getTextChannelById(submissionChannelId);

        if (output == null) {
            throw new BotErrorException("Fan mail service not accessible");
        }

        output.sendMessage(data)
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
        reply.ok("Fanart service started");
    }

    @Override
    public void stop(Reply reply) {
        super.stop(reply);
        reply.send(Icon.STOP, "Fanart service stopped");
    }

    @Override
    public String getStatus() {
        return String.format("""
                Enabled: %b
                Destination: %s
                Review channel: %s
                Timeout: %d seconds
                """,
            isEnabled(),
            Optional.ofNullable(getDestination()).map(IMentionable::getAsMention).orElse(null),
            Optional.ofNullable(submissionChannelId)
                .map(s -> getServer().getGuild().getTextChannelById(s))
                .orElse(null),
            timeout
        );
    }

}
