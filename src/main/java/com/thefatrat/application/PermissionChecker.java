package com.thefatrat.application;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

import java.util.function.Predicate;

public final class PermissionChecker {

    public static final Predicate<Member> IS_ADMIN = member -> {
        if (member.getPermissions().contains(Permission.ADMINISTRATOR)) {
            return true;
        }
        for (Role role : member.getRoles()) {
            if (role.hasPermission(Permission.ADMINISTRATOR)) {
                return true;
            }
        }
        return false;
    };

    public static Predicate<Member> hasAnyRole(Role... roles) {
        return member -> {
            for (Role role : member.getRoles()) {
                for (Role role2 : roles) {
                    if (role.getId().equals(role2.getId())) {
                        return true;
                    }
                }
            }
            return false;
        };
    }

}
