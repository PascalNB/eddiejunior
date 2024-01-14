package com.thefatrat.eddiejunior.handlers;

import com.thefatrat.eddiejunior.entities.PermissionEntity;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class PermissionMapHandler<T, R> extends MapHandler<T, R> {

    private final Map<String, PermissionEntity.RequiredPermission> permissions = new HashMap<>();

    public void addRequiredPermission(String key, PermissionEntity.RequiredPermission requiredPermission) {
        permissions.put(key, requiredPermission);
    }

    @Nullable
    public PermissionEntity.RequiredPermission getRequiredPermission(String key) {
        return permissions.get(key);
    }

}
