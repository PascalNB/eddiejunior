package com.thefatrat.eddiejunior.components.impl;

import com.thefatrat.eddiejunior.components.AbstractComponent;
import com.thefatrat.eddiejunior.entities.Command;
import com.thefatrat.eddiejunior.entities.PermissionEntity;
import com.thefatrat.eddiejunior.events.CommandEvent;
import com.thefatrat.eddiejunior.reply.InteractionReply;
import com.thefatrat.eddiejunior.sources.Server;
import com.thefatrat.eddiejunior.util.Colors;
import com.thefatrat.eddiejunior.util.PermissionChecker;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.attribute.ISlowmodeChannel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.NotNull;

public class ChannelComponent extends AbstractComponent {

    public ChannelComponent(Server server) {
        super(server, "Channel");

        addCommands(
            new Command("slowmode", "set the slow mode for the current channel")
                .setRequiredPermission(PermissionEntity.RequiredPermission.MANAGE)
                .addOptions(
                    new OptionData(OptionType.INTEGER, "time", "time in seconds", true)
                        .setMaxValue(ISlowmodeChannel.MAX_SLOWMODE)
                )
                .setAction(this::setSlowMode)
        );
    }

    /**
     * Sets the slowmode of the current channel to {@code time}.
     *
     * @param command command
     * @param reply   reply
     */
    private void setSlowMode(@NotNull CommandEvent command, InteractionReply reply) {
        TextChannel channel = command.getChannel().asTextChannel();
        PermissionChecker.requirePermission(channel, Permission.MANAGE_CHANNEL);

        int time = command.get("time").getAsInt();
        channel.getManager().setSlowmode(time).queue();

        reply.ok("Set slow mode to %d seconds", time);
        getServer().log(Colors.GRAY, command.getMember().getUser(), "Set slow mode of %s (`%s`) to `%d` " +
            "seconds", channel.getAsMention(), channel.getId(), time);
    }

    @Override
    public String getStatus() {
        return String.format("Enabled: " + isEnabled());
    }

}
