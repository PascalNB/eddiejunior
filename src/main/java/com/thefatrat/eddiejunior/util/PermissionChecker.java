package com.thefatrat.eddiejunior.util;

import com.thefatrat.eddiejunior.exceptions.BotErrorException;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.attribute.IPermissionContainer;
import net.dv8tion.jda.internal.utils.PermissionUtil;
import org.jetbrains.annotations.NotNull;

public final class PermissionChecker {

    public static void requireSend(IPermissionContainer container) throws BotErrorException {
        requirePermission(container, Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_SEND_IN_THREADS,
            Permission.MESSAGE_SEND);
    }

    public static void requirePermission(@NotNull IPermissionContainer container,
        @NotNull Permission @NotNull ... permissions)
    throws BotErrorException {
        Member member = container.getGuild().getSelfMember();
        for (Permission permission : permissions) {
            if (!PermissionUtil.checkPermission(container, member, permission)) {
                throw new BotErrorException("Requires permission `%s`", permission.getName());
            }
        }
    }

    public static void requireMemberPermission(Member member, Permission... permissions) {
        for (Permission permission : permissions) {
            if (!PermissionUtil.checkPermission(member, permission)) {
                throw new BotErrorException("Requires permission `%s`", permission.getName());
            }
        }
    }

}
