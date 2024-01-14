package com.thefatrat.eddiejunior.entities;

public interface PermissionEntity {

    enum RequiredPermission {
        USE, MANAGE, ADMIN
    }

    RequiredPermission getRequiredPermission();

}
