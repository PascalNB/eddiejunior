package com.thefatrat.eddiejunior.events;

import net.dv8tion.jda.api.entities.Member;

public class InteractionEvent<T> {

    private final T entity;
    private final Member member;

    public InteractionEvent(T entity, Member member) {
        this.entity = entity;
        this.member = member;
    }

    public T getEntity() {
        return entity;
    }

    public Member getMember() {
        return member;
    }

}
