package com.thefatrat.application.components;

import com.thefatrat.application.entities.Command;
import com.thefatrat.application.events.CommandEvent;
import com.thefatrat.application.exceptions.BotWarningException;
import com.thefatrat.application.sources.Server;
import com.thefatrat.application.util.Colors;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.RoleIcon;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.utils.ImageProxy;
import net.dv8tion.jda.api.utils.TimeFormat;

import java.util.EnumSet;

public class Grab extends Component {

    public static final String NAME = "Grab";

    public Grab(Server server) {
        super(server, NAME, false);

        addCommands(new Command(getName(), "component command")
            .setAction((command, reply) -> getSubCommandHandler().handle(command.toSub(), reply))
            .addSubcommand(new Command("servericon", "Get the server icon")
                .setAction((command, reply) -> {
                    ImageProxy icon = getServer().getGuild().getIcon();
                    if (icon == null) {
                        throw new BotWarningException("This server does not have an icon");
                    }

                    String url = icon.getUrl(1024);
                    reply.sendEmbed(new EmbedBuilder()
                        .setColor(Colors.BLUE)
                        .setImage(url)
                        .build());
                })
            )
            .addSubcommand(new Command("usericon", "Get a user's icon")
                .addOption(new OptionData(OptionType.USER, "user", "user", false))
                .setAction((command, reply) -> {
                    User user = getEffectiveUser(command);
                    String url = user.getEffectiveAvatar().getUrl(1024);
                    reply.sendEmbed(new EmbedBuilder()
                        .setColor(Colors.BLUE)
                        .setAuthor(user.getAsTag(), null)
                        .setImage(url)
                        .build());
                })
            )
            .addSubcommand(new Command("banner", "Get the server banner")
                .setAction((command, reply) -> {
                    ImageProxy banner = getServer().getGuild().getBanner();
                    if (banner == null) {
                        throw new BotWarningException("This server does not have a banner");
                    }

                    String url = banner.getUrl(1024);
                    reply.sendEmbed(new EmbedBuilder()
                        .setColor(Colors.BLUE)
                        .setImage(url)
                        .build());
                })
            )
            .addSubcommand(new Command("splash", "Get the server splash image")
                .setAction((command, reply) -> {
                    ImageProxy splash = getServer().getGuild().getSplash();
                    if (splash == null) {
                        throw new BotWarningException("This server does not have a splash image");
                    }

                    String url = splash.getUrl(1024);
                    reply.sendEmbed(new EmbedBuilder()
                        .setColor(Colors.BLUE)
                        .setImage(url)
                        .build());
                })
            )
            .addSubcommand(new Command("profile", "Get a user's profile")
                .addOption(new OptionData(OptionType.USER, "user", "user", false))
                .setAction((command, reply) -> {
                    User user = getEffectiveUser(command);
                    User.Profile profile = user.retrieveProfile().complete();

                    EmbedBuilder embed = new EmbedBuilder()
                        .setColor(profile.getAccentColorRaw())
                        .setThumbnail(user.getEffectiveAvatarUrl())
                        .setTitle(user.getAsTag())
                        .setDescription(user.getAsMention());

                    EnumSet<User.UserFlag> flags = user.getFlags();

                    if (!flags.isEmpty()) {
                        embed.addField("Badges", String.join(", ", flags.stream()
                                .map(flag -> '`' + flag.getName() + '`')
                                .toArray(String[]::new)),
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
                            String roles = String.join(", ", member.getRoles().stream()
                                .map(Role::getAsMention)
                                .toArray(String[]::new));

                            embed.addField("Roles", roles, false);
                        } else {
                            embed.addField("Roles", "None", false);
                        }
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

                    reply.sendEmbed(embed.build());
                })
            )
            .addSubcommand(new Command("role", "Get a role's info")
                .addOption(new OptionData(OptionType.ROLE, "role", "role", true))
                .setAction((command, reply) -> {
                    Role role = command.getArgs().get("role").getAsRole();

                    EmbedBuilder embed = new EmbedBuilder()
                        .setColor(role.getColor())
                        .setTitle(role.getName())
                        .setDescription(role.getAsMention())
                        .addField("Color", '#' + Integer.toHexString(role.getColorRaw()), true)
                        .addField("Permissions",
                            String.join(", ", role.getPermissions().stream()
                                .map(permission -> '`' + permission.getName() + '`')
                                .toArray(String[]::new)
                            ), false);

                    RoleIcon icon = role.getIcon();
                    if (icon != null) {
                        embed.setThumbnail(icon.getIconUrl());
                    }

                    embed.setFooter(role.getId());

                    reply.sendEmbed(embed.build());
                })
            )
            .addSubcommand(new Command("permissions", "Get a user's permissions")
                .addOption(new OptionData(OptionType.USER, "user", "user", false))
                .setAction((command, reply) -> {
                    User user = getEffectiveUser(command);

                    Member member = getServer().getGuild().retrieveMemberById(user.getId())
                        .onErrorMap(e -> null)
                        .complete();

                    if (member == null) {
                        throw new BotWarningException("The given user is not a member of this server");
                    }

                    EmbedBuilder embed = new EmbedBuilder()
                        .setColor(member.getColorRaw())
                        .addField("Permissions",
                            String.join(", ", member.getPermissions().stream()
                                .map(permission -> '`' + permission.getName() + '`')
                                .toArray(String[]::new)
                            ), false)
                        .setAuthor(user.getAsTag(), null, member.getEffectiveAvatarUrl())
                        .setFooter(user.getId());

                    reply.sendEmbed(embed.build());
                })
            )
        );

    }

    private User getEffectiveUser(CommandEvent event) {
        User user;
        if (event.getArgs().containsKey("user")) {
            user = event.getArgs().get("user").getAsUser();
        } else {
            user = event.getUser();
        }
        return user;
    }

    @Override
    public String getStatus() {
        return String.format("""
            Enabled: %b
            """, isEnabled());
    }

}
