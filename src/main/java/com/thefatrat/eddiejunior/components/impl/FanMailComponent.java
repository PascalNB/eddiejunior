package com.thefatrat.eddiejunior.components.impl;

import com.thefatrat.eddiejunior.entities.Command;
import com.thefatrat.eddiejunior.entities.PermissionEntity;
import com.thefatrat.eddiejunior.events.ButtonEvent;
import com.thefatrat.eddiejunior.events.CommandEvent;
import com.thefatrat.eddiejunior.exceptions.BotErrorException;
import com.thefatrat.eddiejunior.exceptions.BotWarningException;
import com.thefatrat.eddiejunior.reply.InteractionReply;
import com.thefatrat.eddiejunior.reply.MenuReply;
import com.thefatrat.eddiejunior.reply.Reply;
import com.thefatrat.eddiejunior.sources.Server;
import com.thefatrat.eddiejunior.util.Colors;
import com.thefatrat.eddiejunior.util.Icon;
import com.thefatrat.eddiejunior.util.PermissionChecker;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.IMentionable;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditData;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FanMailComponent extends DirectMessageComponent {

    private static final Set<String> contentTypes = Set.of("image/jpeg", "image/png", "image/jpg", "image/gif");

    private final Map<String, Long> timeouts = new ConcurrentHashMap<>();

    private String submissionChannelId;
    private long timeout;

    public FanMailComponent(Server server) {
        super(server, "Fanart", "Fanart Submissions", true);

        timeout = Long.parseLong(getDatabaseManager().getSettingOrDefault("timeout", "0"));
        submissionChannelId = getDatabaseManager().getSetting("submissionchannel");

        addSubcommands(
            new Command("timeout", "sets the timeout")
                .addOptions(new OptionData(OptionType.INTEGER, "timeout", "timeout in seconds", true)
                    .setRequiredRange(0, Integer.MAX_VALUE)
                )
                .setAction(this::setTimeout),

            new Command("reset", "allow submissions for users again")
                .setRequiredPermission(PermissionEntity.RequiredPermission.USE)
                .addOptions(new OptionData(OptionType.USER, "user", "user", true))
                .setAction(this::resetSubmission),

            new Command("submissionchannel", "sets the channel were submissions will be posted to for review")
                .addOptions(new OptionData(OptionType.CHANNEL, "channel", "channel", true)
                    .setChannelTypes(ChannelType.TEXT)
                )
                .setAction(this::setSubmissionChannel)
        );

        getServer().getButtonHandler().addListener(this::handleButton);
    }

    /**
     * Sets the submission timeout.
     *
     * @param command command
     * @param reply   reply
     */
    private void setTimeout(CommandEvent command, InteractionReply reply) {
        long timeout = command.get("timeout").getAsInt();
        this.timeout = timeout;
        reply.ok("Timout set to %d seconds", timeout);
        getDatabaseManager().setSetting("timeout", String.valueOf(timeout));
        getServer().log(command.getMember().getUser(), "Set fanart timeout to `%d` seconds", timeout);
    }

    /**
     * Resets the submission timeout for the given user.
     *
     * @param command command
     * @param reply   reply
     */
    private void resetSubmission(CommandEvent command, InteractionReply reply) {
        Member member = command.get("user").getAsMember();

        if (member == null) {
            throw new BotErrorException("The given member was not found");
        }

        if (timeouts.remove(member.getId()) == null) {
            throw new BotWarningException("The given user was not on a cooldown");
        }
        reply.send(Icon.RESET, "Cooldown reset for %s, they can submit again", member.getAsMention());
    }

    /**
     * Sets the channel where submissions will be posted for review.
     *
     * @param command command
     * @param reply   reply
     */
    private void setSubmissionChannel(CommandEvent command, InteractionReply reply) {
        TextChannel channel = command.get("channel").getAsChannel().asTextChannel();
        submissionChannelId = channel.getId();

        PermissionChecker.requireSend(channel);

        reply.ok("Set submission review channel to %s", channel.getAsMention());
        getDatabaseManager().setSetting("submissionchannel", channel.getId());
        getServer().log(command.getMember().getUser(), "Set fanart submission review channel to %s " +
            "(`%s`)", channel.getAsMention(), channel.getId());
    }

    private void handleButton(ButtonEvent<Member> event, MenuReply reply) {
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

            try {
                response.addReaction(Emoji.fromUnicode("❤️")).queue();
            } catch (InsufficientPermissionException ignore) {
            }

            MessageEmbed embed = event.getMessage().getEmbeds().get(0);

            MessageCreateData newData = MessageCreateBuilder.from(data)
                .setEmbeds(new EmbedBuilder(embed)
                    .setColor(Colors.GREEN)
                    .build())
                .build();

            event.getMessage().editMessage(MessageEditData.fromCreateData(newData)).queue();

            reply.hide();
            reply.ok("Submission approved\n%s", response.getJumpUrl());

            String userId = Objects.requireNonNull(embed.getFooter()).getText();
            getServer().log(event.getActor().getUser(), "Approved fanart submission by <@%s> (`%s`)",
                userId, userId);

        } else if (event.getButtonId().equals("fanart_deny")) {
            Message message = event.getMessage();
            MessageEmbed embed = message.getEmbeds().get(0);

            MessageCreateData newData = MessageCreateBuilder.fromMessage(message)
                .setComponents()
                .setEmbeds(new EmbedBuilder(embed)
                    .setColor(Colors.RED)
                    .build())
                .build();

            message.editMessage(MessageEditData.fromCreateData(newData)).queue();

            reply.hide();
            reply.send(Icon.STOP, "Denied fanart submission");

            String userId = Objects.requireNonNull(embed.getFooter()).getText();
            getServer().log(event.getActor().getUser(), "Denied fanart submission by <@%s> (`%s`)",
                userId, userId);
        }
    }

    @Override
    protected void handleDirect(Message message, MenuReply reply) {
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

        EmbedBuilder embedBuilder = new EmbedBuilder()
            .setAuthor(message.getAuthor().getEffectiveName(), null, message.getAuthor().getEffectiveAvatarUrl())
            .setColor(Colors.TRANSPARENT)
            .setImage(attachment.getProxyUrl())
            .setFooter(message.getAuthor().getId());

        String description = message.getContentRaw();
        if (!description.isBlank()) {
            if (description.length() > 200) {
                throw new BotWarningException("Submission description cannot be longer than 200 characters");
            }

            embedBuilder.setDescription(String.format("%s:\n%s",
                message.getAuthor().getAsMention(), description));
        } else {
            embedBuilder.setDescription(message.getAuthor().getAsMention());
        }

        MessageCreateData data = new MessageCreateBuilder()
            .addEmbeds(embedBuilder.build())
            .addActionRow(
                Button.success("fanart_approve", "Approve").withEmoji(Emoji.fromUnicode("✔️")),
                Button.danger("fanart_deny", "Deny").withEmoji(Emoji.fromUnicode("✖️"))
            )
            .build();

        TextChannel output = getGuild().getTextChannelById(submissionChannelId);

        if (output == null) {
            throw new BotErrorException("Fanart service not accessible");
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
                Running: %b
                Destination: %s
                Review channel: %s
                Timeout: %d seconds
                """,
            isEnabled(), isRunning(),
            Optional.ofNullable(getDestination()).map(IMentionable::getAsMention).orElse(null),
            Optional.ofNullable(submissionChannelId)
                .map(s -> getGuild().getTextChannelById(s))
                .orElse(null),
            timeout
        );
    }

}
