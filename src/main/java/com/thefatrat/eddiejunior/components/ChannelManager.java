package com.thefatrat.eddiejunior.components;

import com.thefatrat.eddiejunior.entities.Command;
import com.thefatrat.eddiejunior.sources.Server;
import com.thefatrat.eddiejunior.util.Colors;
import com.thefatrat.eddiejunior.util.PermissionChecker;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.attribute.ISlowmodeChannel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class ChannelManager extends Component {

    public ChannelManager(Server server) {
        super(server, "Channel", false);

        addCommands(
            new Command("slowmode", "set the slow mode for the current channel")
                .addOption(new OptionData(OptionType.INTEGER, "time", "time in seconds", true)
                    .setMaxValue(ISlowmodeChannel.MAX_SLOWMODE))
                .setAction((command, reply) -> {
                    TextChannel channel = command.getChannel().asTextChannel();
                    PermissionChecker.requirePermission(channel, Permission.MANAGE_CHANNEL);

                    int time = command.getArgs().get("time").getAsInt();
                    channel.getManager().setSlowmode(time).queue();

                    reply.ok("Set slow mode to %d seconds", time);
                    getServer().log(Colors.GRAY, command.getMember().getUser(), "Set slow mode of %s (`%s`) to `%d` " +
                        "seconds", channel.getAsMention(), channel.getId(), time);
                }),

            new Command("closechannel", "closes the current channel")
                .setAction((command, reply) -> {
                    TextChannel channel = command.getChannel().asTextChannel();
                    PermissionChecker.requirePermission(channel, Permission.MANAGE_PERMISSIONS);

                    channel.getPermissionContainer().getPermissionContainer()
                        .upsertPermissionOverride(getServer().getGuild().getPublicRole())
                        .deny(Permission.VIEW_CHANNEL)
                        .queue(x -> {
                            reply.hide();
                            reply.ok("Closed channel");
                            getServer().log(Colors.RED, command.getMember().getUser(), "Closed %s (`%s`)",
                                channel.getAsMention(), channel.getId());
                        });
                }),

            new Command("openchannel", "opens the current channel")
                .setAction((command, reply) -> {
                    TextChannel channel = command.getChannel().asTextChannel();
                    PermissionChecker.requirePermission(channel, Permission.MANAGE_PERMISSIONS);

                    channel.getPermissionContainer().getPermissionContainer()
                        .upsertPermissionOverride(getServer().getGuild().getPublicRole())
                        .grant(Permission.VIEW_CHANNEL)
                        .queue(x -> {
                            reply.hide();
                            reply.ok("Opened channel");
                            getServer().log(Colors.GREEN, command.getMember().getUser(), "Opened %s (`%s`)",
                                channel.getAsMention(), channel.getId());
                        });
                })
        );
    }

}
