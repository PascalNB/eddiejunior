package com.thefatrat.eddiejunior.components;

import com.thefatrat.eddiejunior.entities.Command;
import com.thefatrat.eddiejunior.entities.Interaction;
import com.thefatrat.eddiejunior.exceptions.BotErrorException;
import com.thefatrat.eddiejunior.exceptions.BotWarningException;
import com.thefatrat.eddiejunior.reply.Reply;
import com.thefatrat.eddiejunior.sources.Server;
import com.thefatrat.eddiejunior.util.Colors;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.internal.utils.PermissionUtil;

import java.util.HashSet;
import java.util.Set;

public class HoistComponent extends Component {

    private final Set<String> set = new HashSet<>();

    public HoistComponent(Server server) {
        super(server, "Hoist", false);

        set.addAll(getDatabaseManager().getSettings("roles"));

        addCommands(
            new Command("hoist", "hoist or unhoist a role")
                .addOptions(new OptionData(OptionType.ROLE, "role", "role", true))
                .setAction((command, reply) -> {
                    Role role = command.get("role").getAsRole();

                    hoist(role, command.getMember().getUser(), reply);
                }),

            new Command("hoistable", "toggle a role to be hoisted or unhoisted by the hoist command")
                .addPermissions(Permission.MANAGE_ROLES)
                .addOptions(new OptionData(OptionType.ROLE, "role", "role", true))
                .setAction((command, reply) -> {
                    Role role = command.get("role").getAsRole();

                    if (!PermissionUtil.checkPermission(getGuild().getSelfMember(),
                        Permission.MANAGE_ROLES)) {
                        throw new BotErrorException("Permission `%s` required", Permission.MANAGE_ROLES.getName());
                    }
                    if (!PermissionUtil.canInteract(getGuild().getSelfMember(), role)) {
                        throw new BotErrorException("Cannot interact with role %s", role.getAsMention());
                    }

                    if (set.contains(role.getId())) {
                        set.remove(role.getId());
                        reply.ok("Disabled role %s from being hoistable", role.getAsMention());
                        getServer().log(Colors.RED, command.getMember().getUser(), "Disabled role %s (`%s`) from " +
                            "being hoistable", role.getAsMention(), role.getId());
                        getDatabaseManager().removeSetting("roles", role.getId());
                    } else {
                        set.add(role.getId());
                        reply.ok("Enabled role %s to be hoistable", role.getAsMention());
                        getServer().log(Colors.GREEN, command.getMember().getUser(), "Enabled role %s (`%s`) to " +
                            "be hoistable", role.getAsMention(), role.getId());
                        getDatabaseManager().addSetting("roles", role.getId());
                    }
                })
        );

        addMemberInteractions(new Interaction<Member>("bot")
            .setAction((event, reply) -> {
                Member target = event.getEntity();
                if (!target.getUser().isBot()) {
                    throw new BotErrorException("The selected member is not a bot");
                }

                Role role = getGuild().getRoleByBot(target.getId());

                if (role == null) {
                    throw new BotErrorException("The selected bot does not have a dedicated role");
                }

                reply.hide();

                hoist(role, event.getMember().getUser(), reply);
            })
        );
    }

    private <T extends Reply> void hoist(Role role, User user, T reply) {
        if (!PermissionUtil.checkPermission(getGuild().getSelfMember(),
            Permission.MANAGE_ROLES)) {
            throw new BotErrorException("Permission `%s` required", Permission.MANAGE_ROLES.getName());
        }
        if (!PermissionUtil.canInteract(getGuild().getSelfMember(), role)) {
            throw new BotErrorException("Cannot interact with role %s", role.getAsMention());
        }

        if (!set.contains(role.getId())) {
            throw new BotWarningException("The given role is not enabled to be altered by this command");
        }

        if (role.isHoisted()) {
            role.getManager().setHoisted(false).queue();
            reply.ok("Unhoisted role %s", role.getAsMention());
            getServer().log(Colors.RED, user, "Unhoisted role %s (`%s`)",
                role.getAsMention(), role.getId());
        } else {
            role.getManager().setHoisted(true).queue();
            reply.ok("Hoisted role %s", role.getAsMention());
            getServer().log(Colors.GREEN, user, "Hoisted role %s (`%s`)",
                role.getAsMention(), role.getId());
        }
    }

}