package com.thefatrat.eddiejunior.components.impl;

import com.thefatrat.eddiejunior.components.AbstractComponent;
import com.thefatrat.eddiejunior.entities.Command;
import com.thefatrat.eddiejunior.entities.UserRole;
import com.thefatrat.eddiejunior.events.CommandEvent;
import com.thefatrat.eddiejunior.events.MessageEvent;
import com.thefatrat.eddiejunior.reply.InteractionReply;
import com.thefatrat.eddiejunior.sources.Server;
import com.thefatrat.eddiejunior.util.Colors;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageReference;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.NotNull;

public class ForwardPurgeComponent extends AbstractComponent {

    private String exclusion;

    public ForwardPurgeComponent(@NotNull Server server) {
        super(server, "forward-purge");

        exclusion = getDatabaseManager().getSetting("exclusionrole");

        setComponentCommand(UserRole.MANAGE);
        addSubcommands(
            new Command("exclude", "set the role that should be allowed to forward messages")
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
            reply.ok("Set the forward purge exclusion role to " + role.getAsMention());
            getServer().log(command.getMember().getUser(), "Set the forward purge exclusion role to %s (`%s`)",
                role.getAsMention(), exclusion);

        } else {
            exclusion = null;
            getDatabaseManager().removeSetting("exclusionrole");
            reply.ok("Removed the forward purge exclusion role");
            getServer().log(command.getMember().getUser(), "Removed the forward purge exclusion role");
        }
    }

    private void handleMessage(MessageEvent event, Void reply) {
        if (!isEnabled()) {
            return;
        }

        MessageReference messageReference = event.getMessage().getMessageReference();
        if (messageReference == null) {
            return;
        }

        if (messageReference.getType() != MessageReference.MessageReferenceType.FORWARD
            || event.getMember().hasPermission(Permission.ADMINISTRATOR)
            || exclusion != null && event.getMember().getRoles().stream()
            .anyMatch(role -> exclusion.equals(role.getId()))) {
            return;
        }

        try {
            Message message = event.getMessage();
            Member author = event.getMember();
            message.delete().queue(__ ->
                getServer().log(Colors.RED, "Deleted forward by %s (`%s`) in %s (`%s`)",
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
