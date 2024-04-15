package com.thefatrat.eddiejunior.components.impl;

import com.thefatrat.eddiejunior.components.AbstractComponent;
import com.thefatrat.eddiejunior.entities.Command;
import com.thefatrat.eddiejunior.entities.UserRole;
import com.thefatrat.eddiejunior.events.CommandEvent;
import com.thefatrat.eddiejunior.events.MessageEvent;
import com.thefatrat.eddiejunior.reply.InteractionReply;
import com.thefatrat.eddiejunior.sources.Server;
import com.thefatrat.eddiejunior.util.Colors;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.NotNull;

public class PollPurgeComponent extends AbstractComponent {

    private String exclusion;

    public PollPurgeComponent(@NotNull Server server) {
        super(server, "poll-purge");

        exclusion = getDatabaseManager().getSetting("exclusionrole");

        setComponentCommand(UserRole.MANAGE);
        addSubcommands(
            new Command("exclude", "set the role that should be allowed to create polls")
                .addOptions(
                    new OptionData(OptionType.ROLE, "role", "The exclusion role", false)
                )
                .setAction(this::setExclusionRole)
        );

        getServer().getMessageHandler().addListener(this::handleMessage);
    }

    private void setExclusionRole(CommandEvent command, InteractionReply reply) {
        if (command.hasOption("role")) {
            Role role = command.get("role").getAsRole();
            exclusion = role.getId();
            getDatabaseManager().setSetting("exclusionrole", exclusion);
            reply.ok("Set the poll purge exclusion role to " + role.getAsMention());
            getServer().log(command.getMember().getUser(), "Set the poll purge exclusion role to %s (`%s`)",
                role.getAsMention(), exclusion);

        } else {
            exclusion = null;
            getDatabaseManager().removeSetting("exclusionrole");
            reply.ok("Removed the poll purge exclusion role");
            getServer().log(command.getMember().getUser(), "Removed the poll purge exclusion role");
        }
    }

    private void handleMessage(MessageEvent event, Void reply) {
        if (!isEnabled()
            || event.getMessage().getPoll() == null
            || exclusion != null && event.getMember().getRoles().stream()
            .anyMatch(role -> exclusion.equals(role.getId()))) {
            return;
        }

        try {
            Message message = event.getMessage();
            Member author = event.getMember();
            message.delete().queue(__ ->
                getServer().log(Colors.RED, "Deleted poll by %s (`%s`) in %s (`%s`)",
                    author.getAsMention(), author.getId(),
                    message.getChannel().getAsMention(), message.getChannelId()
                )
            );
        } catch (InsufficientPermissionException ignore) {
        }
    }

    @Override
    public String getStatus() {
        return String.format("""
                Enabled: %s
                Excluded: <@&%s>
                """,
            isEnabled(), exclusion);
    }

}
