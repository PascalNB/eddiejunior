package com.thefatrat.eddiejunior.components.impl;

import com.thefatrat.eddiejunior.components.AbstractComponent;
import com.thefatrat.eddiejunior.entities.Command;
import com.thefatrat.eddiejunior.entities.UserRole;
import com.thefatrat.eddiejunior.events.ButtonEvent;
import com.thefatrat.eddiejunior.events.CommandEvent;
import com.thefatrat.eddiejunior.exceptions.BotErrorException;
import com.thefatrat.eddiejunior.exceptions.BotWarningException;
import com.thefatrat.eddiejunior.reply.InteractionReply;
import com.thefatrat.eddiejunior.reply.MenuReply;
import com.thefatrat.eddiejunior.sources.Server;
import com.thefatrat.eddiejunior.util.Colors;
import com.thefatrat.eddiejunior.util.EmojiUtil;
import com.thefatrat.eddiejunior.util.PermissionChecker;
import com.thefatrat.eddiejunior.util.URLUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.LayoutComponent;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.internal.utils.PermissionUtil;

import java.util.*;

public class RoleComponent extends AbstractComponent {

    public static final String NAME = "Role";
    private final Set<String> roles;
    public static final Set<String> FILE_MIMES = Set.of("image/jpg", "image/png", "image/jpeg");

    public RoleComponent(Server server) {
        super(server, NAME);

        List<String> roles = getDatabaseManager().getSettings("toggle");
        this.roles = new HashSet<>(roles);

        setComponentCommand(UserRole.MANAGE);

        getServer().getButtonHandler().addListener(this::handleButton);

        addSubcommands(
            new Command("message", "create a message with buttons for a specific role")
                .addPermissions(Permission.MANAGE_ROLES)
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
                .setAction(this::createMessage),

            new Command("settoggle", "enable or disable a role to be toggled")
                .addPermissions(Permission.MANAGE_ROLES)
                .addOptions(
                    new OptionData(OptionType.ROLE, "role", "role", true)
                )
                .setAction(this::setRoleToggle),

            new Command("toggle", "toggle the role of a user")
                .setRequiredUserRole(UserRole.USE)
                .addOptions(
                    new OptionData(OptionType.USER, "user", "user", true),
                    new OptionData(OptionType.ROLE, "role", "role", true),
                    new OptionData(OptionType.ROLE, "role2", "role2", false)
                )
                .setAction(this::toggleRole),

            new Command("list", "list all the toggleable roles")
                .setRequiredUserRole(UserRole.USE)
                .setAction(this::listToggles),

            new Command("seticon", "set a role's icon")
                .setRequiredUserRole(UserRole.MANAGE)
                .addOptions(
                    new OptionData(OptionType.ROLE, "role", "role", true),
                    new OptionData(OptionType.ATTACHMENT, "icon", "icon", true)
                )
                .setAction(this::setRoleIcon)
        );
    }

    private void listToggles(CommandEvent command, InteractionReply reply) {
        if (roles.isEmpty()) {
            throw new BotWarningException("List of toggleable roles is empty");
        }

        StringBuilder builder = new StringBuilder();

        for (String role : roles) {
            builder.append("<@&").append(role).append(">\n");
        }

        String content = builder.deleteCharAt(builder.length() - 1).toString();

        reply.send(new EmbedBuilder()
            .setTitle("Toggleable roles")
            .setDescription(content)
            .setColor(Colors.TRANSPARENT)
            .build());
    }

    /**
     * Handles the role button, toggling the role for the user.
     *
     * @param event button event
     * @param reply reply
     */
    private void handleButton(ButtonEvent<Member> event, MenuReply reply) {
        String buttonId = event.getButtonId();
        if (!buttonId.startsWith("roles-")) {
            return;
        }

        if (!isEnabled()) {
            throw new BotErrorException("Could not assign/remove at the moment");
        }

        String[] split = buttonId.split("-");
        String roleId = split[2];
        Role role = getGuild().getRoleById(roleId);

        if (role == null || role.isManaged() || role.isPublicRole()) {
            throw new BotErrorException("Role could not be assigned/removed");
        }

        if (!PermissionUtil.checkPermission(getGuild().getSelfMember(), Permission.MANAGE_ROLES)) {
            throw new BotErrorException("Permission `%s` required", Permission.MANAGE_ROLES.getName());
        }
        if (!PermissionUtil.canInteract(getGuild().getSelfMember(), role)) {
            throw new BotErrorException("No permission to interact with role %s", role.getAsMention());
        }

        Member member = event.getActor();

        reply.hide();
        if ("1".equals(split[1])) {
            if (member.getRoles().contains(role)) {
                throw new BotWarningException("You already have the " + role.getAsMention() + " role");
            }
            getGuild().addRoleToMember(member, role).queue(success -> {
                reply.ok("You received role " + role.getAsMention());
                getServer().log("Gave role %s (`%s`) to %s (`%s`) (`%s`)", role.getAsMention(), role.getId(),
                    member.getAsMention(), member.getUser().getEffectiveName(), member.getId());
            });
        } else {
            if (!member.getRoles().contains(role)) {
                throw new BotWarningException("You do not have the " + role.getAsMention() + " role");
            }
            getGuild().removeRoleFromMember(member, role).queue(success -> {
                reply.ok("Role " + role.getAsMention() + " has been removed");
                getServer().log("Removed role %s (`%s`) from %s (`%s`) (`%s`)", role.getAsMention(), role.getId(),
                    member.getAsMention(), member.getUser().getEffectiveName(), member.getId());
            });
        }
    }

    /**
     * Creates a role toggle message with buttons.
     *
     * @param command command
     * @param reply   reply
     */
    private void createMessage(CommandEvent command, InteractionReply reply) {
        Member member = command.getMember();

        if (!member.hasPermission(Permission.MANAGE_ROLES)) {
            throw new BotErrorException("You need permission `%s`", Permission.MANAGE_ROLES.getName());
        }

        Role role = command.get("role").getAsRole();

        if (role.equals(getGuild().getPublicRole())) {
            throw new BotErrorException("Cannot use %s as a role", role.getAsMention());
        }
        if (role.isManaged()) {
            throw new BotErrorException("%s is managed by an integration", role.getAsMention());
        }

        OptionMapping labelAddObject = command.get("label_add");
        String labelAdd = labelAddObject == null
            ? "Add @" + role.getName()
            : labelAddObject.getAsString();
        OptionMapping labelRemoveObject = command.get("label_remove");
        String labelRemove = labelRemoveObject == null
            ? "Remove @" + role.getName()
            : labelRemoveObject.getAsString();

        MessageCreateBuilder builder = new MessageCreateBuilder();

        OptionMapping messageOption = command.get("message");

        if (messageOption == null) {
            boolean embedEmpty = true;
            EmbedBuilder embed = new EmbedBuilder()
                .setColor(Colors.TRANSPARENT);

            OptionMapping contentOption = command.get("content");
            if (contentOption != null) {
                embedEmpty = false;
                embed.setDescription(contentOption.getAsString().replaceAll("\\\\n", "\n"));
            }
            OptionMapping titleOption = command.get("title");
            if (titleOption != null) {
                embedEmpty = false;
                embed.setTitle(titleOption.getAsString());
            }

            if (!embedEmpty) {
                builder.addEmbeds(embed.build());
            }
        } else {
            String embedUrl = messageOption.getAsString();
            Message message = URLUtil.messageFromURL(embedUrl, getGuild());

            try (MessageCreateData data = MessageCreateData.fromMessage(message)) {
                builder.applyData(data);
                builder.setComponents();
            } catch (IllegalArgumentException e) {
                throw new BotErrorException("Couldn't use referenced message");
            } catch (IllegalStateException e) {
                throw new BotErrorException("Cannot reference an empty message");
            }
        }

        LayoutComponent component = ActionRow.of(
            EmojiUtil.formatButton("roles-1-" + role.getId(), labelAdd, ButtonStyle.SUCCESS),
            EmojiUtil.formatButton("roles-0-" + role.getId(), labelRemove, ButtonStyle.DANGER)
        );

        builder.addComponents(component);

        if (builder.isEmpty()) {
            throw new BotErrorException("Cannot reference a message without content");
        }

        Optional.ofNullable(command.get("channel"))
            .ifPresentOrElse(
                object -> {
                    TextChannel channel = object.getAsChannel().asTextChannel();
                    PermissionChecker.requireSend(channel);
                    channel.sendMessage(builder.build()).queue();
                    reply.ok("Message sent in %s", channel.getAsMention());
                },
                () -> reply.send(builder.build())
            );
    }

    /**
     * Enables or disables a role from being toggled.
     *
     * @param command command
     * @param reply   reply
     */
    private void setRoleToggle(CommandEvent command, InteractionReply reply) {
        Member member = command.getMember();

        if (!member.hasPermission(Permission.MANAGE_ROLES)) {
            throw new BotErrorException("You need permission `%s`", Permission.MANAGE_ROLES.getName());
        }

        Role role = command.get("role").getAsRole();

        if (role.equals(getGuild().getPublicRole())) {
            throw new BotErrorException("Cannot use %s as a role", role.getAsMention());
        }
        if (role.isManaged()) {
            throw new BotErrorException("%s is managed by an integration", role.getAsMention());
        }

        String id = role.getId();

        if (roles.contains(id)) {
            roles.remove(id);
            getDatabaseManager().removeSetting("toggle", id);
            reply.ok("Disabled role %s from being toggled", role.getAsMention());
            getServer().log(Colors.RED, command.getMember().getUser(), "Disabled role %s (`%s`) from being toggled",
                role.getAsMention(), role.getId());
        } else {
            roles.add(id);
            getDatabaseManager().addSetting("toggle", id);
            reply.ok("Enabled role %s to be toggled", role.getAsMention());
            getServer().log(Colors.GREEN, command.getMember().getUser(), "Enabled role %s (`%s`) to be toggled",
                role.getAsMention(), role.getId());
        }
    }

    /**
     * Adds or removes the given role to the given user.
     *
     * @param command command
     * @param reply   reply
     */
    private void toggleRole(CommandEvent command, InteractionReply reply) {
        if (!PermissionUtil.checkPermission(getGuild().getSelfMember(), Permission.MANAGE_ROLES)) {
            throw new BotErrorException("Permission `%s` required", Permission.MANAGE_ROLES.getName());
        }

        Member member = command.get("user").getAsMember();
        if (member == null) {
            throw new BotWarningException("Member not found");
        }

        List<Role> roles = new ArrayList<>();
        roles.add(command.get("role").getAsRole());
        if (command.hasOption("role2")) {
            roles.add(command.get("role2").getAsRole());
        }

        for (Role role : roles) {
            if (!this.roles.contains(role.getId()) || !PermissionUtil.canInteract(getGuild().getSelfMember(), role)) {
                throw new BotWarningException("Cannot toggle role %s", role.getAsMention());
            }
        }

        List<Role> userRoles = member.getRoles();

        for (Role role : roles) {
            if (userRoles.contains(role)) {
                getGuild().removeRoleFromMember(member, role).queue(c -> {

                    reply.ok("Removed role %s from %s", role.getAsMention(), member.getAsMention());

                    getServer().log(command.getMember().getUser(), "Removed role %s (`%s`) from %s (`%s`)",
                        role.getAsMention(), role.getId(), member.getAsMention(), member.getId());
                });

            } else {
                getGuild().addRoleToMember(member, role).queue(c -> {
                    reply.ok("Gave role %s to %s", role.getAsMention(), member.getAsMention());

                    getServer().log(command.getMember().getUser(), "Gave role %s (`%s`) to %s (`%s`)",
                        role.getAsMention(), role.getId(), member.getAsMention(), member.getId());
                });
            }
        }

    }

    private void setRoleIcon(CommandEvent command, InteractionReply reply) {
        if (!PermissionUtil.checkPermission(getGuild().getSelfMember(), Permission.MANAGE_ROLES)) {
            throw new BotErrorException("Permission `%s` required", Permission.MANAGE_ROLES.getName());
        }
        if (getGuild().getBoostTier().ordinal() < 2) {
            throw new BotErrorException("Server needs tier 2 boosting");
        }

        Role role = command.get("role").getAsRole();
        if (!PermissionUtil.canInteract(getGuild().getSelfMember(), role)) {
            throw new BotErrorException("Cannot modify role %s", role.getAsMention());
        }

        Message.Attachment attachment = command.get("icon").getAsAttachment();
        if (attachment.getSize() > 256 * 1024) {
            throw new BotWarningException("Attachment size cannot be larger than 264KB");
        }
        if (!attachment.isImage() || !FILE_MIMES.contains(attachment.getContentType())) {
            throw new BotErrorException("Only PNG or JPG allowed");
        }

        Icon icon = attachment.getProxy()
            .downloadAsIcon()
            .join();

        if (icon == null) {
            return;
        }

        role.getManager().setIcon(icon).queue(__ -> {
            reply.ok("Icon of role %s set", role.getAsMention());
            getServer().log(command.getMember().getUser(), "Changed role icon of %s (`%s`):%n%s",
                role.getAsMention(), role.getId(),
                Optional.ofNullable(role.getIcon())
                    .map(RoleIcon::getIconUrl)
                    .orElse("null"));
        });
    }

    @Override
    public String getStatus() {
        return String.format("Enabled: " + isEnabled());
    }

}
