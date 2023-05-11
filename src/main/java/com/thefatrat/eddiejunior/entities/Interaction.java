package com.thefatrat.eddiejunior.entities;

import com.thefatrat.eddiejunior.events.InteractionEvent;
import com.thefatrat.eddiejunior.reply.InteractionReply;
import net.dv8tion.jda.api.Permission;
import org.jetbrains.annotations.Contract;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;

public class Interaction<E> {

    private String name;
    private BiConsumer<InteractionEvent<E>, InteractionReply> action = (e, r) -> {};
    private final List<Permission> permissions = new ArrayList<>();

    public Interaction(String name) {this.name = name;}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BiConsumer<InteractionEvent<E>, InteractionReply> getAction() {
        return action;
    }

    @Contract("_ -> this")
    public Interaction<E> setAction(BiConsumer<InteractionEvent<E>, InteractionReply> action) {
        this.action = action;
        return this;
    }

    public Interaction<E> addPermissions(Permission... permissions) {
        Collections.addAll(this.permissions, permissions);
        return this;
    }

    public List<Permission> getPermissions() {
        return permissions;
    }

}
