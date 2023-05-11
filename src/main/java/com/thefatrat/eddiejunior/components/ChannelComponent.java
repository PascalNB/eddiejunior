package com.thefatrat.eddiejunior.components;

import com.thefatrat.eddiejunior.entities.Command;
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

public class ChannelComponent extends Component {

    public ChannelComponent(Server server) {
        super(server, "Channel", false);

        addCommands(
            new Command("slowmode", "set the slow mode for the current channel")
                .addOptions(new OptionData(OptionType.INTEGER, "time", "time in seconds", true)
                    .setMaxValue(ISlowmodeChannel.MAX_SLOWMODE))
                .setAction(this::setSlowMode)
        );
    }

    private void setSlowMode(CommandEvent command, InteractionReply reply) {
        TextChannel channel = command.getChannel().asTextChannel();
        PermissionChecker.requirePermission(channel, Permission.MANAGE_CHANNEL);

        int time = command.get("time").getAsInt();
        channel.getManager().setSlowmode(time).queue();

        reply.ok("Set slow mode to %d seconds", time);
        getServer().log(Colors.GRAY, command.getMember().getUser(), "Set slow mode of %s (`%s`) to `%d` " +
            "seconds", channel.getAsMention(), channel.getId(), time);
    }

}
