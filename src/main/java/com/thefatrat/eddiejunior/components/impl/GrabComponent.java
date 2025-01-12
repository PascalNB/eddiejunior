package com.thefatrat.eddiejunior.components.impl;

import com.thefatrat.eddiejunior.components.AbstractComponent;
import com.thefatrat.eddiejunior.entities.Command;
import com.thefatrat.eddiejunior.entities.Interaction;
import com.thefatrat.eddiejunior.entities.UserRole;
import com.thefatrat.eddiejunior.events.CommandEvent;
import com.thefatrat.eddiejunior.exceptions.BotErrorException;
import com.thefatrat.eddiejunior.exceptions.BotWarningException;
import com.thefatrat.eddiejunior.sources.Server;
import com.thefatrat.eddiejunior.util.Colors;
import com.thefatrat.eddiejunior.util.Icon;
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
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.ImageProxy;
import net.dv8tion.jda.api.utils.Result;
import net.dv8tion.jda.api.utils.TimeFormat;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.List;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class GrabComponent extends AbstractComponent {

    public static final String NAME = "Grab";

    private static final Predicate<String> hexMatcher = Pattern.compile("^[\\da-f]{6}$").asMatchPredicate();

    public GrabComponent(Server server) {
        super(server, NAME);

        setComponentCommand(UserRole.USE);

        addSubcommands(
            new Command("servericon", "Get the server icon")
                .setAction((command, reply) -> {
                    ImageProxy icon = getGuild().getIcon();
                    if (icon == null) {
                        throw new BotWarningException("This server does not have an icon");
                    }

                    String url = icon.getUrl(1024);
                    reply.hide();
                    reply.send(new EmbedBuilder()
                        .setColor(Colors.TRANSPARENT)
                        .setImage(url)
                        .build());
                }),

            new Command("usericon", "Get a user's icon")
                .addOptions(new OptionData(OptionType.USER, "user", "user", false))
                .setAction((command, reply) -> {
                    User user = getEffectiveUser(command);
                    String url = user.getEffectiveAvatar().getUrl(1024);
                    reply.hide();
                    reply.send(new EmbedBuilder()
                        .setColor(Colors.TRANSPARENT)
                        .setAuthor(user.getEffectiveName(), null)
                        .setImage(url)
                        .build());
                }),

            new Command("membericon", "Get a member's guild icon")
                .addOptions(new OptionData(OptionType.USER, "user", "user", false))
                .setAction((command, reply) -> {
                    User user = getEffectiveUser(command);
                    Member member = getGuild().retrieveMember(user)
                        .onErrorMap(e -> null)
                        .complete();

                    if (member == null) {
                        throw new BotErrorException("Member not found");
                    }

                    String url = member.getEffectiveAvatar().getUrl(1024);
                    reply.hide();
                    reply.send(new EmbedBuilder()
                        .setColor(Colors.TRANSPARENT)
                        .setAuthor(user.getEffectiveName(), null)
                        .setImage(url)
                        .build());
                }),

            new Command("banner", "Get the server banner")
                .setAction((command, reply) -> {
                    ImageProxy banner = getGuild().getBanner();
                    if (banner == null) {
                        throw new BotWarningException("This server does not have a banner");
                    }

                    String url = banner.getUrl(1024);
                    reply.hide();
                    reply.send(new EmbedBuilder()
                        .setColor(Colors.TRANSPARENT)
                        .setImage(url)
                        .build());
                }),

            new Command("splash", "Get the server splash image")
                .setAction((command, reply) -> {
                    ImageProxy splash = getGuild().getSplash();
                    if (splash == null) {
                        throw new BotWarningException("This server does not have a splash image");
                    }

                    String url = splash.getUrl(1024);
                    reply.hide();
                    reply.send(new EmbedBuilder()
                        .setColor(Colors.TRANSPARENT)
                        .setImage(url)
                        .build());
                }),

            new Command("profile", "Get a user's profile")
                .addOptions(new OptionData(OptionType.USER, "user", "user", false))
                .setAction((command, reply) -> {
                    User user = getEffectiveUser(command);
                    User.Profile profile = user.retrieveProfile().complete();

                    EmbedBuilder embed = new EmbedBuilder()
                        .setThumbnail(user.getEffectiveAvatarUrl())
                        .setTitle(user.getEffectiveName())
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

                    Member member = getGuild().retrieveMemberById(user.getId())
                        .onErrorMap(e -> null)
                        .complete();

                    if (member != null) {
                        String nick = member.getNickname();
                        if (nick != null) {
                            embed.addField("Nickname", nick, false);
                        }

                        if (!member.getRoles().isEmpty()) {
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

                    reply.hide();
                    reply.send(embed.build());
                }),

            new Command("role", "Get a role's info")
                .addOptions(new OptionData(OptionType.ROLE, "role", "role", true))
                .setAction((command, reply) -> {
                    net.dv8tion.jda.api.entities.Role role = command.get("role").getAsRole();

                    EnumSet<Permission> permissions = role.getPermissions();
                    List<String> permissionNames = new ArrayList<>(permissions.size());
                    for (Permission permission : permissions) {
                        permissionNames.add('`' + permission.getName() + '`');
                    }

                    String hexColor = String.format("#%6s", Integer.toHexString(role.getColorRaw()))
                        .replace(' ', '0');

                    EmbedBuilder embed = new EmbedBuilder()
                        .setColor(role.getColor())
                        .setTitle(role.getName())
                        .setDescription(role.getAsMention())
                        .addField("Color", hexColor, true)
                        .addField("Permissions", String.join(", ", permissionNames.toArray(new String[0])), false);

                    RoleIcon icon = role.getIcon();
                    if (icon != null) {
                        embed.setThumbnail(icon.getIconUrl());
                    }

                    embed.setFooter(role.getId());

                    reply.hide();
                    reply.send(embed.build());
                }),

            new Command("permissions", "Get a user's permissions")
                .addOptions(new OptionData(OptionType.USER, "user", "user", false))
                .setAction((command, reply) -> {
                    User user = getEffectiveUser(command);

                    Member member = getGuild().retrieveMemberById(user.getId())
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
                        .setAuthor(user.getEffectiveName(), null, member.getEffectiveAvatarUrl())
                        .setFooter(user.getId());

                    reply.hide();
                    reply.send(embed.build());
                }),

            new Command("emoji", "Get an emoji")
                .addOptions(new OptionData(OptionType.STRING, "emoji", "emoji", true))
                .setAction((command, reply) -> {
                    String string = command.get("emoji").getAsString().trim();

                    if (!string.matches("^<a?:[a-z\\dA-Z_]+:\\d+>$")) {
                        throw new BotErrorException("Please select a proper emoji");
                    }

                    CustomEmoji emoji = Emoji.fromFormatted(string).asCustom();
                    String url = emoji.getImageUrl();

                    reply.hide();
                    reply.send(new MessageCreateBuilder()
                        .addEmbeds(new EmbedBuilder()
                            .setColor(Colors.TRANSPARENT)
                            .setTitle(emoji.getName())
                            .setFooter(emoji.getId())
                            .setImage(url)
                            .build())
                        .addContent(url)
                        .build());
                }),

            new Command("id", "Get an id")
                .addOptions(new OptionData(OptionType.MENTIONABLE, "mention", "mention", true))
                .setAction((command, reply) -> {
                    IMentionable mention = command.get("mention").getAsMentionable();
                    reply.hide();
                    reply.send(mention.getId());
                }),

            new Command("channel", "Get a channel's info")
                .addOptions(new OptionData(OptionType.CHANNEL, "channel", "channel", true))
                .setAction((command, reply) -> {
                    GuildChannelUnion channel = command.get("channel").getAsChannel();

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
                            actions.add(getGuild().retrieveMemberById(id).mapToResult());
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

                    try {
                        if (!roleOverrides.isEmpty()) {
                            int iteration = 1;
                            for (int i = 0; i < roleOverrides.size(); i++) {
                                StringBuilder builder = new StringBuilder();
                                while (i < roleOverrides.size()) {
                                    String override = roleOverrides.get(i) + "\n\n";
                                    if (builder.length() + override.length() > 1024) {
                                        break;
                                    }
                                    builder.append(override);
                                    i++;
                                }
                                if (builder.isEmpty()) {
                                    break;
                                }
                                builder.delete(builder.length() - 2, builder.length());
                                embed.addField("Role overrides " + iteration, builder.toString(), false);
                                iteration++;
                            }
                        }
                        if (!memberOverrides.isEmpty()) {
                            int iteration = 1;
                            for (int i = 0; i < memberOverrides.size(); i++) {
                                StringBuilder builder = new StringBuilder();
                                while (i < memberOverrides.size()) {
                                    String override = memberOverrides.get(i) + "\n\n";
                                    if (builder.length() + override.length() > 1024) {
                                        break;
                                    }
                                    builder.append(override);
                                    i++;
                                }
                                if (builder.isEmpty()) {
                                    break;
                                }
                                builder.delete(builder.length() - 2, builder.length());
                                embed.addField("Member overrides " + iteration, builder.toString(), false);
                                iteration++;
                            }
                        }
                    } catch (Exception e) {
                        throw new BotErrorException(e.getMessage());
                    }
                    embed.setFooter(channel.getId());
                    reply.hide();
                    reply.send(embed.build());
                }),

            new Command("color", "grab a hex color")
                .addOptions(
                    new OptionData(OptionType.STRING, "hex", "hex", true)
                        .setRequiredLength(6, 7)
                )
                .setAction((command, reply) -> {
                    String hexString = command.get("hex").getAsString().toLowerCase(Locale.ROOT);

                    if (hexString.charAt(0) == '#') {
                        hexString = hexString.substring(1);
                    }

                    if (!hexMatcher.test(hexString)) {
                        throw new BotErrorException("Not a valid hex");
                    }

                    int color = Integer.parseInt(hexString, 16);
                    BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
                    Graphics2D g = image.createGraphics();
                    g.setColor(new Color(color));
                    g.fillRect(0, 0, 100, 100);

                    try {
                        PipedInputStream inputStream = new PipedInputStream();
                        OutputStream outputStream = new PipedOutputStream(inputStream);
                        ImageIO.write(image, "jpg", outputStream);
                        outputStream.close();

                        reply.send(new MessageCreateBuilder()
                            .setFiles(FileUpload.fromData(inputStream, "color.jpg"))
                            .build());

                    } catch (IOException e) {
                        throw new BotErrorException("Error: %s", e.getMessage());
                    }
                }),

            new Command("roles", "list all the roles in this server")
                .setAction((command, reply) ->
                    reply.send(new EmbedBuilder()
                        .setDescription(
                            getGuild().getRoles()
                                .stream()
                                .map(Role::getAsMention)
                                .collect(Collectors.joining("\n"))
                        )
                        .setColor(Colors.TRANSPARENT)
                        .build()
                    )
                )
        );

        addMessageInteractions(
            new Interaction<Message>("sticker", "grab a sticker")
                .setRequiredUserRole(UserRole.USE)
                .setAction((event, reply) -> {
                    List<StickerItem> stickers = event.getEntity().getStickers();

                    if (stickers.isEmpty()) {
                        throw new BotWarningException("No sticker found in the message");
                    }

                    StickerItem sticker = stickers.get(0);

                    reply.hide();
                    reply.send(new MessageCreateBuilder()
                        .addEmbeds(new EmbedBuilder()
                            .setColor(Colors.TRANSPARENT)
                            .setTitle(sticker.getName())
                            .setFooter(sticker.getId())
                            .setImage(sticker.getIcon().getUrl(512))
                            .build())
                        .addContent(sticker.getIcon().getUrl(512))
                        .build());
                })
        );

    }

    private User getEffectiveUser(@NotNull CommandEvent event) {
        User user;
        if (event.hasOption("user")) {
            user = event.get("user").getAsUser();
        } else {
            user = event.getMember().getUser();
        }
        return user;
    }

    @NotNull
    private String formatPermissions(@NotNull Iterable<Permission> permissions) {
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

    @NotNull
    private String formatOverrides(@NotNull IMentionable permissionHolder, @NotNull PermissionOverride override) {
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
        return String.format("Enabled: " + isEnabled());
    }

}
