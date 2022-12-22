package com.thefatrat.application.events;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;

public class ButtonEvent {

    private final Member member;
    private final String buttonId;
    private final Message message;

    public ButtonEvent(Member member, String buttonId, Message message) {
        this.member = member;
        this.buttonId = buttonId;
        this.message = message;
    }

    public Member getMember() {
        return member;
    }

    public String getButtonId() {
        return buttonId;
    }

    public Message getMessage() {
        return message;
    }

}
