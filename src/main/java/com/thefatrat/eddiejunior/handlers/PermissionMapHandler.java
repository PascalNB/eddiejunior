package com.thefatrat.eddiejunior.handlers;

import com.thefatrat.eddiejunior.entities.UserRole;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class PermissionMapHandler<T, R> extends MapHandler<T, R> {

    private final Map<String, UserRole> permissions = new HashMap<>();

    public void addRequiredPermission(String key, UserRole userRole) {
        permissions.put(key, userRole);
    }

    @Nullable
    public UserRole getRequiredPermission(String key) {
        return permissions.get(key);
    }

}
