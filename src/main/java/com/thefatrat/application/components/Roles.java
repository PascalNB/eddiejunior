package com.thefatrat.application.components;

import com.thefatrat.application.entities.Command;
import com.thefatrat.application.exceptions.BotErrorException;
import com.thefatrat.application.sources.Server;
import com.thefatrat.application.util.Colors;
import com.thefatrat.application.util.EmojiChecker;
import com.thefatrat.application.util.PermissionChecker;
import com.thefatrat.application.util.URLUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.LayoutComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.internal.utils.PermissionUtil;

public class Roles extends Component {

    public static final String NAME = "Role";

    public Roles(Server server) {
        super(server, NAME, false);

        setComponentCommand(Permission.MANAGE_ROLES);

        getServer().getButtonHandler().addListener((event, reply) -> {
            String buttonId = event.getButtonId();
            if (!buttonId.startsWith("roles-")) {
                return;
            }

            if (!isEnabled()) {
                throw new BotErrorException("Could not assign/remove at the moment");
            }

            String[] split = buttonId.split("-");
            String roleId = split[2];
            Role role = getServer().getGuild().getRoleById(roleId);

            if (role == null || role.isManaged() || role.isPublicRole()) {
                throw new BotErrorException("Role could not be assigned/removed");
            }

            if (!PermissionUtil.checkPermission(getServer().getGuild().getSelfMember(), Permission.MANAGE_ROLES)) {
                throw new BotErrorException("Permission `%s` required", Permission.MANAGE_ROLES.getName());
            }
            if (!PermissionUtil.canInteract(getServer().getGuild().getSelfMember(), role)) {
                throw new BotErrorException("No permission to interact with role %s", role.getAsMention());
            }

            Member member = event.getUser();

            if ("1".equals(split[1])) {
                getServer().getGuild().addRoleToMember(member, role).queue(success ->
                    reply.getSender().hide().ok("You received role " + role.getAsMention())
                );
            } else {
                getServer().getGuild().removeRoleFromMember(member, role).queue(success ->
                    reply.getSender().hide().ok("Role " + role.getAsMention() + " has been removed")
                );
            }
        });

        addSubcommands(
            new Command("message", "create a message with buttons for a specific role")
                .addOptions(
                    new OptionData(OptionType.ROLE, "role", "role", true),
                    new OptionData(OptionType.STRING, "label_add", "button label for adding role, " +
                        "use | to split an emoji and label, e.g. \uD83D\uDE0E|label", false)
                        .setMaxLength(80),
                    new OptionData(OptionType.STRING, "label_remove", "button label for removing role," +
                        "use | to split an emoji and label, e.g. \uD83D\uDE0E|label", false)
                        .setMaxLength(80),

                    new OptionData(OptionType.CHANNEL, "channel", "channel to send the message in", false)
                        .setChannelTypes(ChannelType.TEXT),

                    new OptionData(OptionType.STRING, "title", "message title", false)
                        .setMaxLength(50),
                    new OptionData(OptionType.STRING, "content", "message content", false)
                        .setMaxLength(1024),

                    new OptionData(OptionType.STRING, "message", "jump url of a message to use as content, " +
                        "overwrites the 'content' and 'title' options", false)
                )
                .setAction((command, reply) -> {
                    Member member = command.getMember();

                    if (!member.hasPermission(Permission.MANAGE_ROLES)) {
                        throw new BotErrorException("You need permission `%s`", Permission.MANAGE_ROLES.getName());
                    }

                    Role role = command.getArgs().get("role").getAsRole();

                    if (role.equals(getServer().getGuild().getPublicRole())) {
                        throw new BotErrorException("Cannot use %s as a role", role.getAsMention());
                    }
                    if (role.isManaged()) {
                        throw new BotErrorException("%s is managed by an integration", role.getAsMention());
                    }

                    OptionMapping labelAddObject = command.getArgs().get("label_add");
                    String labelAdd = labelAddObject == null
                        ? "Add @" + role.getName()
                        : labelAddObject.getAsString();
                    OptionMapping labelRemoveObject = command.getArgs().get("label_remove");
                    String labelRemove = labelRemoveObject == null
                        ? "Remove @" + role.getName()
                        : labelRemoveObject.getAsString();

                    MessageCreateBuilder builder = new MessageCreateBuilder();

                    OptionMapping messageOption = command.getArgs().get("message");

                    if (messageOption == null) {
                        boolean embedEmpty = true;
                        EmbedBuilder embed = new EmbedBuilder()
                            .setColor(Colors.TRANSPARENT);

                        OptionMapping contentOption = command.getArgs().get("content");
                        if (contentOption != null) {
                            embedEmpty = false;
                            embed.setDescription(contentOption.getAsString().replaceAll("\\\\n", "\n"));
                        }
                        OptionMapping titleOption = command.getArgs().get("title");
                        if (titleOption != null) {
                            embedEmpty = false;
                            embed.setTitle(titleOption.getAsString());
                        }

                        if (!embedEmpty) {
                            builder.addEmbeds(embed.build());
                        }
                    } else {
                        String embedUrl = messageOption.getAsString();
                        Message message = URLUtil.messageFromURL(embedUrl, getServer().getGuild());

                        try (MessageCreateData data = MessageCreateData.fromMessage(message)) {
                            builder.applyData(data);
                            builder.setComponents();
                        } catch (IllegalArgumentException e) {
                            throw new BotErrorException("Couldn't use referenced message");
                        } catch (IllegalStateException e) {
                            throw new BotErrorException("Cannot reference an empty message");
                        }
                    }

                    Button buttonAdd;
                    Button buttonRemove;
                    String[] splitAdd = labelAdd.split(" ?\\| ?", 2);
                    String[] splitRemove = labelRemove.split(" ?\\| ?", 2);

                    if (EmojiChecker.isEmoji(splitAdd[0])) {
                        Emoji emoji = Emoji.fromUnicode(splitAdd[0]);
                        if (splitAdd.length > 1) {
                            buttonAdd = Button.success("roles-1-" + role.getId(), splitAdd[1]).withEmoji(emoji);
                        } else {
                            buttonAdd = Button.success("roles-1-" + role.getId(), emoji);
                        }
                    } else {
                        buttonAdd = Button.success("roles-1-" + role.getId(), labelAdd);
                    }

                    if (EmojiChecker.isEmoji(splitRemove[0])) {
                        Emoji emoji = Emoji.fromUnicode(splitRemove[0]);
                        if (splitRemove.length > 1) {
                            buttonRemove = Button.danger("roles-0-" + role.getId(), splitRemove[1]).withEmoji(emoji);
                        } else {
                            buttonRemove = Button.danger("roles-0-" + role.getId(), emoji);
                        }
                    } else {
                        buttonRemove = Button.danger("roles-0-" + role.getId(), labelRemove);
                    }

                    LayoutComponent component = ActionRow.of(buttonAdd, buttonRemove);

                    builder.addComponents(component);

                    if (builder.isEmpty()) {
                        throw new BotErrorException("Cannot reference a message without content");
                    }

                    OptionMapping channelObject = command.getArgs().get("channel");

                    if (channelObject == null) {
                        reply.accept(builder.build());
                        return;
                    }

                    TextChannel channel = channelObject.getAsChannel().asTextChannel();

                    PermissionChecker.requireSend(channel);

                    channel.sendMessage(builder.build()).queue();
                    reply.ok("Message sent in %s", channel.getAsMention());
                })
        );
    }

}
