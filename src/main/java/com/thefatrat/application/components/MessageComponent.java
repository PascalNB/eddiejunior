package com.thefatrat.application.components;

import com.thefatrat.application.entities.Command;
import com.thefatrat.application.exceptions.BotErrorException;
import com.thefatrat.application.sources.Server;
import com.thefatrat.application.util.PermissionChecker;
import com.thefatrat.application.util.URLUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import net.dv8tion.jda.api.utils.messages.MessageEditData;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class MessageComponent extends Component {

    public MessageComponent(Server server) {
        super(server, "Message", false);

        setComponentCommand(Permission.MESSAGE_MANAGE);

        addSubcommands(
            new Command("edit", "edit a message sent by me")
                .addOptions(
                    new OptionData(OptionType.STRING, "message", "message jump url", true),
                    new OptionData(OptionType.STRING, "regex", "regex", true),
                    new OptionData(OptionType.STRING, "target", "target", true)
                )
                .setAction((command, reply) -> {
                    String regex = command.getArgs().get("regex").getAsString();
                    try {
                        Pattern.compile(regex);
                    } catch (PatternSyntaxException e) {
                        throw new BotErrorException("Please input a valid regex");
                    }

                    String url = command.getArgs().get("message").getAsString();
                    Message message = URLUtil.messageFromURL(url, getServer().getGuild());

                    if (!message.getAuthor().getId().equals(getServer().getGuild().getSelfMember().getId())) {
                        throw new BotErrorException("Message was not sent by me");
                    }

                    PermissionChecker.requireSend(
                        message.getChannel().asGuildMessageChannel().getPermissionContainer());

                    MessageEditBuilder builder = new MessageEditBuilder();

                    try (MessageEditData data = MessageEditData.fromMessage(message)) {
                        builder.applyData(data);
                        String content = data.getContent();

                        String target = command.getArgs().get("target").getAsString();
                        String newContent = content.replaceAll(regex, target);

                        if (newContent.isBlank()) {
                            throw new BotErrorException("Cannot create a blank message");
                        }

                        builder.setContent(newContent);
                    } catch (IllegalArgumentException e) {
                        throw new BotErrorException("Couldn't use referenced message");
                    } catch (IllegalStateException e) {
                        throw new BotErrorException("Cannot reference an empty message");
                    }

                    message.editMessage(builder.build())
                        .onErrorMap(e -> null)
                        .queue(m -> {
                            if (m == null) {
                                reply.hide().except(new BotErrorException("Couldn't edit message"));
                            } else {
                                reply.ok("Message was edited");
                            }
                        });
                }),

            new Command("remove", "remove a message sent by me")
                .addOptions(new OptionData(OptionType.STRING, "message", "message jump url", true))
                .setAction((command, reply) -> {
                    String url = command.getArgs().get("message").getAsString();
                    Message message = URLUtil.messageFromURL(url, getServer().getGuild());

                    if (!message.getAuthor().getId().equals(getServer().getGuild().getSelfMember().getId())) {
                        throw new BotErrorException("Message was not sent by me");
                    }

                    message.delete().queue(success -> reply.hide().ok("Removed message"));
                })
        );

    }

}
