package com.thefatrat.eddiejunior.components.impl;

import com.thefatrat.eddiejunior.components.AbstractComponent;
import com.thefatrat.eddiejunior.entities.Command;
import com.thefatrat.eddiejunior.entities.Interaction;
import com.thefatrat.eddiejunior.entities.PermissionEntity;
import com.thefatrat.eddiejunior.exceptions.BotErrorException;
import com.thefatrat.eddiejunior.exceptions.BotWarningException;
import com.thefatrat.eddiejunior.sources.Server;
import com.thefatrat.eddiejunior.util.Colors;
import com.thefatrat.eddiejunior.util.PermissionChecker;
import com.thefatrat.eddiejunior.util.URLUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.ForumChannel;
import net.dv8tion.jda.api.entities.channel.forums.ForumPost;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import net.dv8tion.jda.api.utils.messages.MessageEditData;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static com.thefatrat.eddiejunior.components.impl.RoleComponent.FILE_MIMES;

public class MessageComponent extends AbstractComponent {

    private final Map<String, Message> clipboard = new HashMap<>();

    public MessageComponent(Server server) {
        super(server, "Message");

        setComponentCommand(PermissionEntity.RequiredPermission.MANAGE);

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

                    Map<String, Object> metadata = Map.of(
                        "channel", message.getChannel().getId(),
                        "message", message.getId()
                    );

                    TextInput input = TextInput.create(
                            "message",
                            "Message", TextInputStyle.PARAGRAPH)
                        .setRequired(true)
                        .setValue(content)
                        .setRequiredRange(1, 2000)
                        .build();

                    reply.sendModal(
                        getRequestManager().createModal("message_edit", "Edit message", metadata)
                            .addActionRow(input)
                            .build()
                    );
                }),

            new Command("send", "send a message in a given channel")
                .setRequiredPermission(PermissionEntity.RequiredPermission.USE)
                .addOptions(
                    new OptionData(OptionType.CHANNEL, "channel", "text channel", true)
                        .setChannelTypes(ChannelType.TEXT, ChannelType.VOICE, ChannelType.NEWS,
                            ChannelType.GUILD_PUBLIC_THREAD, ChannelType.GUILD_PRIVATE_THREAD),
                    new OptionData(OptionType.STRING, "reply", "reply message url", false),
                    new OptionData(OptionType.BOOLEAN, "ping", "ping replied user", false)
                )
                .setAction((command, reply) -> {
                    MessageChannel channel = command.get("channel").getAsChannel().asGuildMessageChannel();

                    if (!channel.canTalk()) {
                        throw new BotWarningException("Cannot talk in the given channel");
                    }

                    Map<String, Object> metadata = Map.of(
                        "channel", channel.getId(),
                        "reply", Optional.ofNullable(command.get("reply"))
                            .map(OptionMapping::getAsString)
                            .orElse(""),
                        "ping", Optional.ofNullable(command.get("ping"))
                            .map(OptionMapping::getAsBoolean)
                            .orElse(true)
                    );

                    TextInput input = TextInput.create("message_send_input", "Text",
                            TextInputStyle.PARAGRAPH)
                        .setRequiredRange(1, 2000)
                        .build();

                    reply.sendModal(
                        getRequestManager().createModal("message_send", "Send message", metadata)
                            .addActionRow(input)
                            .build()
                    );
                }),

            new Command("post", "create a new forum post in the given channel")
                .addOptions(
                    new OptionData(OptionType.CHANNEL, "channel", "forum channel", true)
                        .setChannelTypes(ChannelType.FORUM),
                    new OptionData(OptionType.ATTACHMENT, "image", "image", false)
                )
                .setAction((command, reply) -> {
                    Message.Attachment attachment = null;
                    if (command.hasOption("image")) {
                        attachment = command.get("image").getAsAttachment();
                        if (attachment.getSize() > 10 * 1024 * 1024) {
                            throw new BotWarningException("Attachment size cannot be larger than 10MB");
                        }
                        if (!attachment.isImage() || !FILE_MIMES.contains(attachment.getContentType())) {
                            throw new BotErrorException("Only PNG or JPG allowed");
                        }
                    }

                    ForumChannel channel = command.get("channel").getAsChannel().asForumChannel();

                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("channel", channel.getId());
                    metadata.put("attachment", attachment);

                    TextInput title = TextInput.create("post_title", "Title",
                            TextInputStyle.SHORT)
                        .setRequiredRange(1, ForumChannel.MAX_NAME_LENGTH)
                        .build();

                    TextInput body = TextInput.create("post_body", "Text",
                            TextInputStyle.PARAGRAPH)
                        .setRequiredRange(1, Message.MAX_CONTENT_LENGTH)
                        .build();

                    reply.sendModal(
                        getRequestManager().createModal("message_post", "Post thread", metadata)
                            .addActionRow(title)
                            .addActionRow(body)
                            .build()
                    );
                }),

            new Command("embed", "send a new embedded message in the given channel")
                .setRequiredPermission(PermissionEntity.RequiredPermission.USE)
                .addOptions(
                    new OptionData(OptionType.CHANNEL, "channel", "channel", true)
                        .setChannelTypes(ChannelType.TEXT, ChannelType.VOICE, ChannelType.NEWS,
                            ChannelType.GUILD_PUBLIC_THREAD, ChannelType.GUILD_PRIVATE_THREAD)
                )
                .setAction((command, reply) -> {
                    MessageChannel channel = command.get("channel").getAsChannel().asGuildMessageChannel();

                    if (!channel.canTalk()) {
                        throw new BotWarningException("Cannot talk in the given channel");
                    }

                    Map<String, Object> metadata = Map.of(
                        "channel", channel.getId()
                    );

                    TextInput title = TextInput.create("embed_title", "Title", TextInputStyle.SHORT)
                        .setRequiredRange(0, MessageEmbed.TITLE_MAX_LENGTH)
                        .setRequired(false)
                        .build();

                    TextInput thumbnail = TextInput.create("embed_thumbnail", "Thumbnail URL",
                            TextInputStyle.SHORT)
                        .setRequiredRange(0, MessageEmbed.URL_MAX_LENGTH)
                        .setRequired(false)
                        .build();

                    TextInput description = TextInput.create("embed_desc", "Description",
                            TextInputStyle.PARAGRAPH)
                        .setRequiredRange(0, Math.min(TextInput.MAX_VALUE_LENGTH, MessageEmbed.DESCRIPTION_MAX_LENGTH))
                        .setRequired(false)
                        .build();

                    TextInput image = TextInput.create("embed_image", "Image URL",
                            TextInputStyle.SHORT)
                        .setRequiredRange(0, MessageEmbed.URL_MAX_LENGTH)
                        .setRequired(false)
                        .build();

                    reply.sendModal(
                        getRequestManager().createModal("message_embed", "Send message embed", metadata)
                            .addActionRow(title)
                            .addActionRow(thumbnail)
                            .addActionRow(description)
                            .addActionRow(image)
                            .build()
                    );
                }),

            new Command("editembed", "edit the first embed in the given message")
                .setRequiredPermission(PermissionEntity.RequiredPermission.MANAGE)
                .addOptions(new OptionData(OptionType.STRING, "message", "message jump url", true))
                .setAction((command, reply) -> {
                    String url = command.get("message").getAsString();
                    Message message = URLUtil.messageFromURL(url, getGuild());

                    if (!message.getAuthor().getId().equals(getGuild().getSelfMember().getId())) {
                        throw new BotErrorException("Message was not sent by me");
                    }

                    if (!message.getChannel().asGuildMessageChannel().canTalk()) {
                        throw new BotErrorException("Cannot talk in the given channel");
                    }

                    if (message.getEmbeds().isEmpty()) {
                        throw new BotWarningException("given message does not have embeds");
                    }

                    MessageEmbed embed = message.getEmbeds().get(0);

                    Map<String, Object> metadata = Map.of(
                        "channel", message.getChannel().getId(),
                        "message", message.getId()
                    );

                    TextInput title = TextInput.create("embed_title", "Title", TextInputStyle.SHORT)
                        .setRequiredRange(0, MessageEmbed.TITLE_MAX_LENGTH)
                        .setValue(embed.getTitle())
                        .setRequired(false)
                        .build();

                    TextInput thumbnail = TextInput.create("embed_thumbnail", "Thumbnail URL",
                            TextInputStyle.SHORT)
                        .setRequiredRange(0, MessageEmbed.URL_MAX_LENGTH)
                        .setValue(
                            Optional.ofNullable(embed.getThumbnail())
                                .map(MessageEmbed.Thumbnail::getUrl)
                                .orElse(null)
                        )
                        .setRequired(false)
                        .build();

                    TextInput description = TextInput.create("embed_desc", "Description",
                            TextInputStyle.PARAGRAPH)
                        .setRequiredRange(0, Math.min(TextInput.MAX_VALUE_LENGTH, MessageEmbed.DESCRIPTION_MAX_LENGTH))
                        .setValue(embed.getDescription())
                        .setRequired(false)
                        .build();

                    TextInput image = TextInput.create("embed_image", "Image URL",
                            TextInputStyle.SHORT)
                        .setRequiredRange(0, MessageEmbed.URL_MAX_LENGTH)
                        .setValue(
                            Optional.ofNullable(embed.getImage())
                                .map(MessageEmbed.ImageInfo::getUrl)
                                .orElse(null)
                        )
                        .setRequired(false)
                        .build();

                    reply.sendModal(
                        getRequestManager().createModal("message_embed_edit", "Edit message embed", metadata)
                            .addActionRow(title)
                            .addActionRow(thumbnail)
                            .addActionRow(description)
                            .addActionRow(image)
                            .build()
                    );
                }),

            new Command("paste", "paste the copied message")
                .setRequiredPermission(PermissionEntity.RequiredPermission.USE)
                .setAction((command, reply) -> {
                    Message message = this.clipboard.get(command.getMember().getId());
                    if (message == null) {
                        throw new BotWarningException("Clipboard is empty");
                    }
                    if (!command.getChannel().canTalk()) {
                        throw new BotErrorException("Cannot talk in the given channel");
                    }
                    try {
                        command.getChannel().sendMessage(MessageCreateData.fromMessage(message)).queue();
                    } catch (Exception e) {
                        throw new BotWarningException(e.getMessage());
                    }
                    reply.hide();
                    reply.ok("Pasted!");
                })
        );

        addMessageInteractions(
            new Interaction<Message>("edit")
                .addPermissions(Permission.MESSAGE_MANAGE)
                .setRequiredPermission(PermissionEntity.RequiredPermission.MANAGE)
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

                    Map<String, Object> metadata = Map.of(
                        "message", message.getId(),
                        "channel", message.getChannel().getId()
                    );

                    TextInput input = TextInput.create("message", "Message", TextInputStyle.PARAGRAPH)
                        .setRequired(true)
                        .setValue(content)
                        .setRequiredRange(1, 2000)
                        .build();

                    reply.sendModal(
                        getRequestManager().createModal("message_edit", "Edit message", metadata)
                            .addActionRow(input)
                            .build()
                    );
                }),

            new Interaction<Message>("editembed")
                .addPermissions(Permission.MESSAGE_MANAGE)
                .setRequiredPermission(PermissionEntity.RequiredPermission.MANAGE)
                .setAction((event, reply) -> {
                    Message message = event.getEntity();

                    if (!message.getAuthor().getId().equals(getGuild().getSelfMember().getId())) {
                        throw new BotErrorException("Message was not sent by me");
                    }

                    if (!message.getChannel().asGuildMessageChannel().canTalk()) {
                        throw new BotErrorException("Cannot talk in the given channel");
                    }

                    if (message.getEmbeds().isEmpty()) {
                        throw new BotWarningException("given message does not have embeds");
                    }

                    MessageEmbed embed = message.getEmbeds().get(0);

                    Map<String, Object> metadata = Map.of(
                        "channel", message.getChannel().getId(),
                        "message", message.getId()
                    );

                    TextInput title = TextInput.create("embed_title", "Title", TextInputStyle.SHORT)
                        .setRequiredRange(0, MessageEmbed.TITLE_MAX_LENGTH)
                        .setValue(embed.getTitle())
                        .setRequired(false)
                        .build();

                    TextInput thumbnail = TextInput.create("embed_thumbnail", "Thumbnail URL",
                            TextInputStyle.SHORT)
                        .setRequiredRange(0, MessageEmbed.URL_MAX_LENGTH)
                        .setValue(
                            Optional.ofNullable(embed.getThumbnail())
                                .map(MessageEmbed.Thumbnail::getUrl)
                                .orElse(null)
                        )
                        .setRequired(false)
                        .build();

                    TextInput description = TextInput.create("embed_desc", "Description",
                            TextInputStyle.PARAGRAPH)
                        .setRequiredRange(0, Math.min(TextInput.MAX_VALUE_LENGTH, MessageEmbed.DESCRIPTION_MAX_LENGTH))
                        .setValue(embed.getDescription())
                        .setRequired(false)
                        .build();

                    TextInput image = TextInput.create("embed_image", "Image URL",
                            TextInputStyle.SHORT)
                        .setRequiredRange(0, MessageEmbed.URL_MAX_LENGTH)
                        .setValue(
                            Optional.ofNullable(embed.getImage())
                                .map(MessageEmbed.ImageInfo::getUrl)
                                .orElse(null)
                        )
                        .setRequired(false)
                        .build();

                    reply.sendModal(
                        getRequestManager().createModal("message_embed_edit", "Edit message embed", metadata)
                            .addActionRow(title)
                            .addActionRow(thumbnail)
                            .addActionRow(description)
                            .addActionRow(image)
                            .build()
                    );
                }),

            new Interaction<Message>("copy")
                .setRequiredPermission(PermissionEntity.RequiredPermission.USE)
                .setAction((event, reply) -> {
                    Message message = event.getEntity();
                    clipboard.put(
                        event.getMember().getId(),
                        message
                    );
                    reply.hide();
                    reply.ok("Copied!");
                })
        );

        getServer().getModalHandler().addListener("message_send", (event, reply) -> {
            String channelId = (String) event.getMetadata().get("channel");
            MessageChannel channel = getGuild().getChannelById(GuildMessageChannel.class, channelId);

            if (channel == null) {
                throw new BotErrorException("Message channel with id `%s` not found", channelId);
            }
            if (!channel.canTalk()) {
                throw new BotWarningException("Cannot send messages in %s", channel.getAsMention());
            }

            String content = event.getValues().get("message_send_input").getAsString();
            MessageCreateAction createAction = channel.sendMessage(content);

            String responseUrl = (String) event.getMetadata().get("reply");
            if (!responseUrl.isBlank()) {
                Message message = URLUtil.messageFromURL(responseUrl, getGuild());
                if (!channel.getId().equals(message.getChannel().getId())) {
                    throw new BotWarningException("Can only reply to messages in the same channel");
                }
                createAction = createAction
                    .setMessageReference(message)
                    .mentionRepliedUser((boolean) event.getMetadata().get("ping"));
            }

            Message response = createAction
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

        getServer().getModalHandler().addListener("message_embed", (event, reply) -> {
            String channelId = (String) event.getMetadata().get("channel");

            MessageChannel channel = getGuild().getChannelById(GuildMessageChannel.class, channelId);
            if (channel == null) {
                throw new BotErrorException("Message channel with id `%s` not found", channelId);
            }
            if (!channel.canTalk()) {
                throw new BotWarningException("Cannot send messages in %s", channel.getAsMention());
            }

            String title = event.getValues().get("embed_title").getAsString();
            String thumbnail = event.getValues().get("embed_thumbnail").getAsString();
            String description = event.getValues().get("embed_desc").getAsString();
            String imageUrl = event.getValues().get("embed_image").getAsString();

            EmbedBuilder builder = new EmbedBuilder()
                .setColor(Colors.TRANSPARENT);

            boolean notEmpty = false;
            if (!title.isBlank()) {
                builder.setTitle(title);
                notEmpty = true;
            }
            if (!thumbnail.isBlank() && URLUtil.matchUrl(thumbnail) != null) {
                builder.setThumbnail(thumbnail);
                notEmpty = true;
            }
            if (!description.isBlank()) {
                builder.setDescription(description);
                notEmpty = true;
            }
            if (!imageUrl.isBlank() && URLUtil.matchUrl(imageUrl) != null) {
                builder.setImage(imageUrl);
                notEmpty = true;
            }

            if (!notEmpty) {
                throw new BotWarningException("Embed cannot be empty");
            }

            Message response = channel.sendMessageEmbeds(builder.build()).complete();

            reply.hide();
            reply.ok("Message embed sent in %s\n%s", channel.getAsMention(), response.getJumpUrl());
            getServer().log(event.getMember().getUser(), "Sent custom embedded message in %s (`%s`)\n%s",
                channel.getAsMention(), channel.getId(), response.getJumpUrl());
        });

        getServer().getModalHandler().addListener("message_post", (event, reply) -> {
            reply.hide();
            reply.defer();

            String channelId = (String) event.getMetadata().get("channel");

            ForumChannel channel = getGuild().getForumChannelById(channelId);
            if (channel == null) {
                throw new BotErrorException("Channel with id `%s` not found", channelId);
            }

            PermissionChecker.requirePermission(channel, Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND);

            String title = event.getValues().get("post_title").getAsString();
            String body = event.getValues().get("post_body").getAsString();

            MessageCreateBuilder builder = new MessageCreateBuilder()
                .setContent(body);

            Message.Attachment attachment = (Message.Attachment) event.getMetadata().get("attachment");
            ForumPost response = null;
            boolean attachmentSuccess = false;
            if (attachment != null) {
                try (InputStream inputStream = attachment.getProxy().download().join()) {
                    builder.setFiles(FileUpload.fromData(inputStream, attachment.getFileName()));

                    response = channel.createForumPost(title, builder.build())
                        .onErrorMap(e -> null)
                        .complete();
                    attachmentSuccess = response != null;
                } catch (IOException ignore) {
                }
            }
            if (!attachmentSuccess) {
                response = channel.createForumPost(title, builder.build())
                    .onErrorMap(e -> null)
                    .complete();
            }

            if (response == null) {
                throw new BotErrorException("Message could not be posted");
            }

            reply.ok("Message posted in %s\n%s", channel.getAsMention(), response.getMessage().getJumpUrl());
            getServer().log(event.getMember().getUser(), "Sent custom post in %s (`%s`)\n%s",
                channel.getAsMention(), channel.getId(), response.getMessage().getJumpUrl());
        });

        getServer().getModalHandler().addListener("message_edit", (event, reply) -> {
            String channelId = (String) event.getMetadata().get("channel");
            String messageId = (String) event.getMetadata().get("message");

            MessageChannel channel = getGuild().getChannelById(GuildMessageChannel.class, channelId);
            if (channel == null) {
                throw new BotErrorException("Message could not be edited");
            }

            if (!channel.canTalk()) {
                throw new BotErrorException("Insufficient permissions");
            }

            Message message = channel.retrieveMessageById(messageId)
                .onErrorMap(e -> null)
                .complete();

            if (message == null) {
                throw new BotErrorException("Message could not be edited");
            }

            String content = event.getValues().get("message").getAsString();
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

        getServer().getModalHandler().addListener("message_embed_edit", (event, reply) -> {
            String channelId = (String) event.getMetadata().get("channel");
            String messageId = (String) event.getMetadata().get("message");

            MessageChannel channel = getGuild().getChannelById(GuildMessageChannel.class, channelId);
            if (channel == null) {
                throw new BotErrorException("Message could not be edited");
            }

            if (!channel.canTalk()) {
                throw new BotErrorException("Insufficient permissions");
            }

            Message message = channel.retrieveMessageById(messageId)
                .onErrorMap(e -> null)
                .complete();

            if (message == null) {
                throw new BotErrorException("Message could not be edited");
            }

            List<MessageEmbed> embeds = new ArrayList<>(message.getEmbeds());
            MessageEmbed newEmbed;
            try {
                String title = event.getValues().get("embed_title").getAsString();
                String description = event.getValues().get("embed_desc").getAsString();
                String image = event.getValues().get("embed_image").getAsString();
                String thumbnail = event.getValues().get("embed_thumbnail").getAsString();
                newEmbed = new EmbedBuilder(embeds.get(0))
                    .setTitle(title.isEmpty() ? null : title)
                    .setDescription(description.isEmpty() ? null : description)
                    .setImage(image.isEmpty() ? null : image)
                    .setThumbnail(thumbnail.isEmpty() ? null : thumbnail)
                    .build();
            } catch (Exception e) {
                throw new BotErrorException(e.getMessage());
            }
            embeds.set(0, newEmbed);

            MessageEditData editData = MessageEditBuilder.fromMessage(message)
                .setEmbeds(embeds)
                .build();

            Message response = message.editMessage(editData)
                .onErrorMap(e -> null)
                .complete();

            if (response == null) {
                throw new BotErrorException("Message could not be edited");
            }

            reply.hide();
            reply.ok("Message embed has been edited");
            getServer().log(event.getMember().getUser(), "Edited message embed in %s (`%s`)\n(%s)",
                channel.getAsMention(), channel.getId(), message.getJumpUrl());
        });
    }

    @Override
    public String getStatus() {
        return String.format("Enabled: " + isEnabled());
    }

}
