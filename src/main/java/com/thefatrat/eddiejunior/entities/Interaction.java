package com.thefatrat.eddiejunior.entities;

import com.thefatrat.eddiejunior.events.InteractionEvent;
import com.thefatrat.eddiejunior.reply.EphemeralReply;
import com.thefatrat.eddiejunior.reply.ModalReply;
import com.thefatrat.eddiejunior.reply.Reply;
import net.dv8tion.jda.api.Permission;
import org.jetbrains.annotations.Contract;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;

public class Interaction<E> {

    private String name;
    private BiConsumer<InteractionEvent<E>, ?> action = (__, ___) -> {};
    private final List<Permission> permissions = new ArrayList<>();

    public Interaction(String name) {this.name = name;}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @SuppressWarnings("unchecked")
    public <T extends Reply & EphemeralReply & ModalReply> BiConsumer<InteractionEvent<E>, T> getAction() {
        return (BiConsumer<InteractionEvent<E>, T>) action;
    }

    @Contract("_ -> this")
    public <T extends Reply & EphemeralReply & ModalReply> Interaction<E> setAction(
        BiConsumer<InteractionEvent<E>, T> action) {
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
