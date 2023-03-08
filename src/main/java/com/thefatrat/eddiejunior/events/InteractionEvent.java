package com.thefatrat.eddiejunior.events;

import net.dv8tion.jda.api.entities.Member;

public class InteractionEvent<T> {

    private final T entity;
    private final Member member;
    private final String action;

    public InteractionEvent(T entity, Member member, String action) {
        this.entity = entity;
        this.member = member;
        this.action = action;
    }

    public T getEntity() {
        return entity;
    }

    public String getAction() {
        return action;
    }

    public Member getMember() {
        return member;
    }

}
