package com.thefatrat.eddiejunior.components.impl;

import com.thefatrat.eddiejunior.components.AbstractComponent;
import com.thefatrat.eddiejunior.entities.Command;
import com.thefatrat.eddiejunior.entities.Interaction;
import com.thefatrat.eddiejunior.exceptions.BotErrorException;
import com.thefatrat.eddiejunior.exceptions.BotWarningException;
import com.thefatrat.eddiejunior.sources.Server;
import com.thefatrat.eddiejunior.util.PermissionChecker;
import com.thefatrat.eddiejunior.util.URLUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.ForumChannel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.forums.ForumPost;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

public class MessageComponent extends AbstractComponent {

    public MessageComponent(Server server) {
        super(server, "Message");

        setComponentCommand();

        addSubcommands(
            new Command("edit", "edit a message sent by me")
                .addOptions(
                    new OptionData(OptionType.STRING, "message", "message jump url", true)
                )
                .setAction((command, reply) -> {
                    String url = command.get("message").getAsString();
                    Message message = URLUtil.messageFromURL(url, getGuild());

                    if (!message.getAuthor().getId().equals(getGuild().getSelfMember().getId())) {
                        throw new BotErrorException("Message was not sent by me");
                    }

                    PermissionChecker.requireSend(
                        message.getChannel().asGuildMessageChannel().getPermissionContainer());

                    String content = message.getContentRaw();

                    if (content.isEmpty()) {
                        throw new BotErrorException("Message does not contain editable text");
                    }

                    TextInput input = TextInput.create(
                            "message_" + message.getChannel().getId() + "_" + message.getId(),
                            "Message", TextInputStyle.PARAGRAPH)
                        .setRequired(true)
                        .setValue(content)
                        .setRequiredRange(1, 2000)
                        .build();

                    reply.sendModal(
                        Modal.create("message_edit", "Edit message")
                            .addActionRow(input)
                            .build()
                    );
                }),

            new Command("send", "send a message in a given channel")
                .addOptions(new OptionData(OptionType.CHANNEL, "channel", "text channel", true)
                    .setChannelTypes(ChannelType.TEXT)
                )
                .setAction((command, reply) -> {
                    TextChannel channel = command.get("channel").getAsChannel().asTextChannel();

                    if (!channel.canTalk()) {
                        throw new BotWarningException("Cannot talk in the given channel");
                    }

                    TextInput input = TextInput.create("message_send_input_" + channel.getId(), "Text",
                            TextInputStyle.PARAGRAPH)
                        .setRequiredRange(1, 2000)
                        .build();

                    reply.sendModal(Modal.create("message_send", "Send message")
                        .addActionRow(input)
                        .build());
                }),

            new Command("post", "create a new forum post in the given channel")
                .addOptions(new OptionData(OptionType.CHANNEL, "channel", "forum channel", true)
                    .setChannelTypes(ChannelType.FORUM)
                )
                .setAction((command, reply) -> {
                    ForumChannel channel = command.get("channel").getAsChannel().asForumChannel();

                    TextInput title = TextInput.create("post_title_" + channel.getId(), "Title",
                            TextInputStyle.SHORT)
                        .setRequiredRange(1, ForumChannel.MAX_NAME_LENGTH)
                        .build();

                    TextInput body = TextInput.create("post_body_" + channel.getId(), "Text",
                            TextInputStyle.PARAGRAPH)
                        .setRequiredRange(1, 2048)
                        .build();

                    reply.sendModal(Modal.create("message_post", "Post thread")
                        .addActionRow(title)
                        .addActionRow(body)
                        .build());
                })
        );

        addMessageInteractions(
            new Interaction<Message>("edit")
                .addPermissions(Permission.MESSAGE_MANAGE)
                .setAction((event, reply) -> {
                    Message message = event.getEntity();

                    if (!message.getAuthor().getId().equals(getGuild().getSelfMember().getId())) {
                        throw new BotErrorException("Message was not sent by me");
                    }

                    PermissionChecker.requireSend(
                        message.getChannel().asGuildMessageChannel().getPermissionContainer());

                    String content = message.getContentRaw();

                    if (content.isEmpty()) {
                        throw new BotErrorException("Message does not contain editable text");
                    }

                    TextInput input = TextInput.create(
                            "message_" + message.getChannel().getId() + "_" + message.getId(),
                            "Message", TextInputStyle.PARAGRAPH)
                        .setRequired(true)
                        .setValue(content)
                        .setRequiredRange(1, 2000)
                        .build();

                    reply.sendModal(
                        Modal.create("message_edit", "Edit message")
                            .addActionRow(input)
                            .build()
                    );
                }),

            new Interaction<Message>("copy")
                .setAction((event, reply) -> {
                    Message message = event.getEntity();
                    String content = message.getContentRaw();

                    if (content.isEmpty()) {
                        throw new BotWarningException("Message is empty");
                    }

                    reply.sendModal(Modal.create("message_copy", "Copy message")
                        .addActionRow(
                            TextInput.create("input", "Raw text", TextInputStyle.PARAGRAPH)
                                .setValue(message.getContentRaw())
                                .build()
                        )
                        .addActionRow(
                            TextInput.create("input2", "Displayed text", TextInputStyle.PARAGRAPH)
                                .setValue(message.getContentStripped())
                                .build()
                        )
                        .build());
                })
        );

        getServer().getModalHandler().addListener("message_send", (event, reply) -> {
            String key = event.getValues().keySet().iterator().next();
            String[] split = key.split("_", 4);

            TextChannel channel = getGuild().getTextChannelById(split[3]);
            if (channel == null) {
                throw new BotErrorException("Channel with id `%s` not found", split[3]);
            }
            if (!channel.canTalk()) {
                throw new BotWarningException("Cannot send messages in %s", channel.getAsMention());
            }

            String content = event.getValues().get(key).getAsString();
            Message response = channel.sendMessage(content)
                .onErrorMap(e -> null)
                .complete();

            if (response == null) {
                throw new BotErrorException("Message could not be sent");
            }
            reply.hide();
            reply.ok("Message sent in %s\n%s", channel.getAsMention(), response.getJumpUrl());
            getServer().log(event.getMember().getUser(), "Sent custom message in %s (`%s`)\n%s",
                channel.getAsMention(), channel.getId(), response.getJumpUrl());
        });

        getServer().getModalHandler().addListener("message_post", (event, reply) -> {
            String key = event.getValues().keySet().iterator().next();
            String[] split = key.split("_", 3);
            String channelId = split[2];

            ForumChannel channel = getGuild().getForumChannelById(channelId);
            if (channel == null) {
                throw new BotErrorException("Channel with id `%s` not found", split[3]);
            }

            PermissionChecker.requirePermission(channel, Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND);

            String title = event.getValues().get("post_title_" + channelId).getAsString();
            String body = event.getValues().get("post_body_" + channelId).getAsString();

            MessageCreateData message = MessageCreateData.fromContent(body);
            ForumPost response = channel.createForumPost(title, message)
                .onErrorMap(e -> null)
                .complete();

            if (response == null) {
                throw new BotErrorException("Message could not be posted");
            }
            reply.hide();
            reply.ok("Message posted in %s\n%s", channel.getAsMention(), response.getMessage().getJumpUrl());
            getServer().log(event.getMember().getUser(), "Sent custom post in %s (`%s`)\n%s",
                channel.getAsMention(), channel.getId(), response.getMessage().getJumpUrl());
        });

        getServer().getModalHandler().addListener("message_edit", (event, reply) -> {
            String key = event.getValues().keySet().iterator().next();
            String[] split = key.split("_", 3);

            TextChannel channel = getGuild().getTextChannelById(split[1]);
            if (channel == null) {
                throw new BotErrorException("Message could not be edited");
            }

            PermissionChecker.requireSend(channel.getPermissionContainer());

            Message message = channel.retrieveMessageById(split[2])
                .onErrorMap(e -> null)
                .complete();

            if (message == null) {
                throw new BotErrorException("Message could not be edited");
            }

            String content = event.getValues().get(key).getAsString();
            Message response = message.editMessage(content)
                .onErrorMap(e -> null)
                .complete();

            if (response == null) {
                throw new BotErrorException("Message could not be edited");
            }
            reply.hide();
            reply.ok("Message has been edited");
            getServer().log(event.getMember().getUser(), "Edited message in %s (`%s`)\n(%s)",
                channel.getAsMention(), channel.getId(), message.getJumpUrl());
        });

        getServer().getModalHandler().addListener("message_copy", (event, reply) -> {
            reply.hide();
            reply.ok("");
        });

    }

    @Override
    public String getStatus() {
        return String.format("Enabled: " + isEnabled());
    }

}
