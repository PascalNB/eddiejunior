package com.thefatrat.eddiejunior.events;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;

public class MessageEvent {

    private final Member member;
    private final Message message;

    public MessageEvent(Member member, Message message) {
        this.member = member;
        this.message = message;
    }

    public Member getMember() {
        return member;
    }

    public Message getMessage() {
        return message;
    }

}
