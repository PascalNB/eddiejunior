package com.thefatrat.eddiejunior.components;

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
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;

public class MessageComponent extends Component {

    public MessageComponent(Server server) {
        super(server, "Message", false);

        setComponentCommand(Permission.MESSAGE_MANAGE);

        addSubcommands(
            new Command("edit", "edit a message sent by me")
                .addOptions(
                    new OptionData(OptionType.STRING, "message", "message jump url", true)
                )
                .setAction((command, reply) -> {

                    String url = command.getArgs().get("message").getAsString();
                    Message message = URLUtil.messageFromURL(url, getServer().getGuild());

                    if (!message.getAuthor().getId().equals(getServer().getGuild().getSelfMember().getId())) {
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

            new Command("remove", "remove a message sent by me")
                .addOptions(new OptionData(OptionType.STRING, "message", "message jump url", true))
                .setAction((command, reply) -> {
                    String url = command.getArgs().get("message").getAsString();
                    Message message = URLUtil.messageFromURL(url, getServer().getGuild());

                    if (!message.getAuthor().getId().equals(getServer().getGuild().getSelfMember().getId())) {
                        throw new BotErrorException("Message was not sent by me");
                    }
                    reply.hide();
                    message.delete().queue(success -> {
                        reply.ok("Removed message");
                        getServer().log(command.getMember().getUser(), "Deleted message\n(%s)", url);
                    });
                }),

            new Command("send", "send a message in a given channel")
                .addOption(new OptionData(OptionType.CHANNEL, "channel", "channel", true)
                    .setChannelTypes(ChannelType.TEXT)
                )
                .setAction((command, reply) -> {
                    TextChannel channel = command.getArgs().get("channel").getAsChannel().asTextChannel();

                    if (!channel.canTalk()) {
                        throw new BotWarningException("Cannot talk in the given channel");
                    }

                    TextInput input = TextInput.create("message_send_input_" + channel.getId(), "Text",
                            TextInputStyle.PARAGRAPH)
                        .setMinLength(1)
                        .build();

                    reply.sendModal(Modal.create("message_send", "Send message")
                        .addActionRow(input)

                        .build());
                })
        );

        addMessageInteractions(
            new Interaction<Message>("edit")
                .setAction((event, reply) -> {
                    Message message = event.getEntity();

                    if (!message.getAuthor().getId().equals(getServer().getGuild().getSelfMember().getId())) {
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

                    content = content.replaceAll("```", "<codeblock>");

                    if (content.length() > 2042) {
                        content = content.substring(0, 2042);
                    }

                    reply.hide();
                    reply.send("```%s```", content);
                })
        );

        getServer().getModalHandler().addListener("message_send", (event, reply) -> {
            String key = event.getValues().keySet().iterator().next();
            String[] split = key.split("_", 4);

            TextChannel channel = getServer().getGuild().getTextChannelById(split[3]);
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

        getServer().getModalHandler().addListener("message_edit", (event, reply) -> {
            String key = event.getValues().keySet().iterator().next();
            String[] split = key.split("_", 3);

            TextChannel channel = getServer().getGuild().getTextChannelById(split[1]);
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

    }

}
