package com.thefatrat.application.components;

import com.thefatrat.application.entities.Command;
import com.thefatrat.application.entities.Interaction;
import com.thefatrat.application.events.CommandEvent;
import com.thefatrat.application.exceptions.BotErrorException;
import com.thefatrat.application.exceptions.BotWarningException;
import com.thefatrat.application.sources.Server;
import com.thefatrat.application.util.Colors;
import com.thefatrat.application.util.Icon;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.unions.GuildChannelUnion;
import net.dv8tion.jda.api.entities.emoji.CustomEmoji;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.entities.sticker.StickerItem;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.utils.ImageProxy;
import net.dv8tion.jda.api.utils.Result;
import net.dv8tion.jda.api.utils.TimeFormat;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;

public class Grab extends Component {

    public static final String NAME = "Grab";

    public Grab(Server server) {
        super(server, NAME, false);

        setComponentCommand();

        addSubcommands(
            new Command("servericon", "Get the server icon")
                .setAction((command, reply) -> {
                    ImageProxy icon = getServer().getGuild().getIcon();
                    if (icon == null) {
                        throw new BotWarningException("This server does not have an icon");
                    }

                    String url = icon.getUrl(1024);
                    reply.hide().accept(new EmbedBuilder()
                        .setColor(Colors.TRANSPARENT)
                        .setImage(url)
                        .build());
                }),

            new Command("usericon", "Get a user's icon")
                .addOption(new OptionData(OptionType.USER, "user", "user", false))
                .setAction((command, reply) -> {
                    User user = getEffectiveUser(command);
                    String url = user.getEffectiveAvatar().getUrl(1024);
                    reply.hide().accept(new EmbedBuilder()
                        .setColor(Colors.TRANSPARENT)
                        .setAuthor(user.getAsTag(), null)
                        .setImage(url)
                        .build());
                }),

            new Command("banner", "Get the server banner")
                .setAction((command, reply) -> {
                    ImageProxy banner = getServer().getGuild().getBanner();
                    if (banner == null) {
                        throw new BotWarningException("This server does not have a banner");
                    }

                    String url = banner.getUrl(1024);
                    reply.hide().accept(new EmbedBuilder()
                        .setColor(Colors.TRANSPARENT)
                        .setImage(url)
                        .build());
                }),

            new Command("splash", "Get the server splash image")
                .setAction((command, reply) -> {
                    ImageProxy splash = getServer().getGuild().getSplash();
                    if (splash == null) {
                        throw new BotWarningException("This server does not have a splash image");
                    }

                    String url = splash.getUrl(1024);
                    reply.hide().accept(new EmbedBuilder()
                        .setColor(Colors.TRANSPARENT)
                        .setImage(url)
                        .build());
                }),

            new Command("profile", "Get a user's profile")
                .addOption(new OptionData(OptionType.USER, "user", "user", false))
                .setAction((command, reply) -> {
                    User user = getEffectiveUser(command);
                    User.Profile profile = user.retrieveProfile().complete();

                    EmbedBuilder embed = new EmbedBuilder()
                        .setThumbnail(user.getEffectiveAvatarUrl())
                        .setTitle(user.getAsTag())
                        .setDescription(user.getAsMention());

                    EnumSet<User.UserFlag> flags = user.getFlags();

                    if (!flags.isEmpty()) {
                        List<String> list = new ArrayList<>(flags.size());
                        for (User.UserFlag flag : flags) {
                            list.add('`' + flag.getName() + '`');
                        }
                        embed.addField("Badges", String.join(", ", list.toArray(new String[0])),
                            false);
                    }

                    Member member = getServer().getGuild().retrieveMemberById(user.getId())
                        .onErrorMap(e -> null)
                        .complete();

                    if (member != null) {
                        String nick = member.getNickname();
                        if (nick != null) {
                            embed.addField("Nickname", nick, false);
                        }

                        if (member.getRoles().size() > 0) {
                            List<net.dv8tion.jda.api.entities.Role> roles = member.getRoles();
                            List<String> mentions = new ArrayList<>(roles.size());
                            for (Role role : roles) {
                                mentions.add(role.getAsMention());
                            }
                            String joined = String.join(" ", mentions.toArray(new String[0]));

                            embed.addField("Role", joined, false);
                        } else {
                            embed.addField("Role", "None", false);
                        }
                        embed.setColor(member.getColorRaw());
                    } else {
                        embed.setColor(profile.getAccentColorRaw());
                    }

                    embed.addField("Account date", TimeFormat.DATE_TIME_LONG.format(user.getTimeCreated()), true);

                    if (member != null) {
                        embed.addField("Join date", TimeFormat.DATE_TIME_LONG.format(member.getTimeJoined()), true);
                    }

                    ImageProxy banner = profile.getBanner();
                    if (banner != null) {
                        embed.setImage(banner.getUrl(1024));
                    }

                    embed.setFooter(user.getId());

                    reply.hide().accept(embed.build());
                }),

            new Command("role", "Get a role's info")
                .addOption(new OptionData(OptionType.ROLE, "role", "role", true))
                .setAction((command, reply) -> {
                    net.dv8tion.jda.api.entities.Role role = command.getArgs().get("role").getAsRole();

                    EnumSet<Permission> permissions = role.getPermissions();
                    List<String> permissionNames = new ArrayList<>(permissions.size());
                    for (Permission permission : permissions) {
                        permissionNames.add('`' + permission.getName() + '`');
                    }
                    EmbedBuilder embed = new EmbedBuilder()
                        .setColor(role.getColor())
                        .setTitle(role.getName())
                        .setDescription(role.getAsMention())
                        .addField("Color", '#' + Integer.toHexString(role.getColorRaw()), true)
                        .addField("Permissions", String.join(", ", permissionNames.toArray(new String[0])), false);

                    RoleIcon icon = role.getIcon();
                    if (icon != null) {
                        embed.setThumbnail(icon.getIconUrl());
                    }

                    embed.setFooter(role.getId());

                    reply.hide().accept(embed.build());
                }),

            new Command("permissions", "Get a user's permissions")
                .addOption(new OptionData(OptionType.USER, "user", "user", false))
                .setAction((command, reply) -> {
                    User user = getEffectiveUser(command);

                    Member member = getServer().getGuild().retrieveMemberById(user.getId())
                        .onErrorMap(e -> null)
                        .complete();

                    if (member == null) {
                        throw new BotWarningException("The given user is not a member of this server");
                    }

                    EnumSet<Permission> permissions = member.getPermissions();
                    List<String> list = new ArrayList<>(permissions.size());
                    for (Permission permission : permissions) {
                        list.add('`' + permission.getName() + '`');
                    }

                    EmbedBuilder embed = new EmbedBuilder()
                        .setColor(member.getColorRaw())
                        .addField("Permissions", String.join(", ", list.toArray(new String[0])), false)
                        .setAuthor(user.getAsTag(), null, member.getEffectiveAvatarUrl())
                        .setFooter(user.getId());

                    reply.hide().accept(embed.build());
                }),

            new Command("emoji", "Get an emoji")
                .addOption(new OptionData(OptionType.STRING, "emoji", "emoji", true))
                .setAction((command, reply) -> {
                    String string = command.getArgs().get("emoji").getAsString().trim();

                    if (!string.matches("^<a?:[a-z\\dA-Z_]+:\\d+>$")) {
                        throw new BotErrorException("Please select a proper emoji");
                    }

                    CustomEmoji emoji = Emoji.fromFormatted(string).asCustom();
                    String url = emoji.getImageUrl();

                    reply.hide().accept(new EmbedBuilder()
                        .setColor(Colors.TRANSPARENT)
                        .setTitle(emoji.getName())
                        .setImage(url)
                        .setFooter(emoji.getId())
                        .build());
                }),

            new Command("id", "Get an id")
                .addOption(new OptionData(OptionType.MENTIONABLE, "mention", "mention", true))
                .setAction((command, reply) -> {
                    IMentionable mention = command.getArgs().get("mention").getAsMentionable();
                    reply.hide().accept(mention.getId());
                }),

            new Command("channel", "Get a channel's info")
                .addOption(new OptionData(OptionType.CHANNEL, "channel", "channel", true))
                .setAction((command, reply) -> {
                    GuildChannelUnion channel = command.getArgs().get("channel").getAsChannel();

                    List<PermissionOverride> rolePermissionOverrides = channel.getPermissionContainer()
                        .getRolePermissionOverrides();
                    List<PermissionOverride> memberPermissionOverrides = channel.getPermissionContainer()
                        .getMemberPermissionOverrides();
                    List<String> roleOverrides = new ArrayList<>();
                    List<String> memberOverrides = new ArrayList<>();

                    for (PermissionOverride override : rolePermissionOverrides) {
                        Role role = override.getRole();
                        if (role != null && (override.getAllowedRaw() != 0L || override.getDeniedRaw() != 0L)) {
                            roleOverrides.add(formatOverrides(role, override));
                        }
                    }
                    if (!memberPermissionOverrides.isEmpty()) {
                        List<RestAction<Result<Member>>> actions = new ArrayList<>();
                        for (PermissionOverride override : memberPermissionOverrides) {
                            String id = override.getId();
                            actions.add(getServer().getGuild().retrieveMemberById(id).mapToResult());
                        }
                        List<Result<Member>> results = RestAction.allOf(actions).complete();
                        for (int i = 0; i < results.size(); ++i) {
                            Result<Member> result = results.get(i);
                            Member member;
                            PermissionOverride override = memberPermissionOverrides.get(i);
                            if (result.isSuccess() && (member = result.get()) != null &&
                                (override.getAllowedRaw() != 0L || override.getDeniedRaw() != 0L)) {
                                memberOverrides.add(formatOverrides(member, override));
                            }
                        }
                    }

                    EmbedBuilder embed = new EmbedBuilder()
                        .setColor(Colors.TRANSPARENT)
                        .setTitle(channel.getName())
                        .setDescription(channel.getAsMention())
                        .addField("Channel type", '`' + channel.getType().toString() + '`', true)
                        .addField("Creation date", TimeFormat.DATE_TIME_LONG.format(channel.getTimeCreated()), true);
                    if (!roleOverrides.isEmpty()) {
                        embed.addField("Role overrides", String.join("\n\n", roleOverrides), false);
                    }
                    if (!memberOverrides.isEmpty()) {
                        embed.addField("Member overrides", String.join("\n\n", memberOverrides), false);
                    }
                    embed.setFooter(channel.getId());
                    reply.hide().accept(embed.build());
                })
        );

        addInteractions(new Interaction("sticker")
            .setAction((command, reply) -> {
                List<StickerItem> stickers = command.getMessage().getStickers();

                if (stickers.size() == 0) {
                    throw new BotWarningException("No sticker found in the message");
                }

                StickerItem sticker = stickers.get(0);

                reply.hide().accept(new EmbedBuilder()
                    .setColor(Colors.TRANSPARENT)
                    .setTitle(sticker.getName())
                    .setImage(sticker.getIcon().getUrl(512))
                    .setFooter(sticker.getId())
                    .build());
            })
        );

    }

    private User getEffectiveUser(CommandEvent event) {
        User user;
        if (event.getArgs().containsKey("user")) {
            user = event.getArgs().get("user").getAsUser();
        } else {
            user = event.getMember().getUser();
        }
        return user;
    }

    private String formatPermissions(Iterable<Permission> permissions) {
        StringBuilder builder = new StringBuilder();
        for (Iterator<Permission> iterator = permissions.iterator(); iterator.hasNext(); ) {
            Permission permission = iterator.next();
            builder.append('`').append(permission.getName()).append('`');
            if (iterator.hasNext()) {
                builder.append(", ");
            }
        }
        return builder.toString();
    }

    private String formatOverrides(IMentionable permissionHolder, PermissionOverride override) {
        StringBuilder builder = new StringBuilder()
            .append(permissionHolder.getAsMention()).append("\n");
        EnumSet<Permission> allowed = override.getAllowed();
        if (!allowed.isEmpty()) {
            builder.append(Icon.OK).append(" ").append(formatPermissions(allowed));
        }
        EnumSet<Permission> denied = override.getDenied();
        if (!allowed.isEmpty() && !denied.isEmpty()) {
            builder.append("\n");
        }
        if (!denied.isEmpty()) {
            builder.append(Icon.ERROR).append(" ").append(formatPermissions(denied));
        }
        return builder.toString();
    }

    @Override
    public String getStatus() {
        return String.format("""
            Enabled: %b
            """, isEnabled());
    }

}
