package com.thefatrat.application;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

import java.util.function.Predicate;

public final class PermissionChecker {

    public static final Predicate<Member> IS_ADMIN = member -> {
        if (member.getPermissions().contains(Permission.MANAGE_SERVER)) {
            return true;
        }
        for (Role role : member.getRoles()) {
            if (role.hasPermission(Permission.MANAGE_SERVER)) {
                return true;
            }
        }
        return false;
    };

    public static Predicate<Member> hasAnyRole(String... roles) {
        return member -> {
            for (Role role : member.getRoles()) {
                for (String id : roles) {
                    if (role.getId().equals(id)) {
                        return true;
                    }
                }
            }
            return false;
        };
    }

}
