package com.thefatrat.eddiejunior.entities;

public interface UserRoleEntity<T> {

    UserRole getRequiredUserRole();

    T setRequiredUserRole(UserRole userRole);

}
