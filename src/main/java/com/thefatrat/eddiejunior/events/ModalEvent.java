package com.thefatrat.eddiejunior.events;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;

import java.util.Map;

public class ModalEvent {

    public final Member member;
    public final Map<String, ModalMapping> values;

    public ModalEvent(Member member, Map<String, ModalMapping> values) {
        this.member = member;
        this.values = values;
    }

    public Member getMember() {
        return member;
    }

    public Map<String, ModalMapping> getValues() {
        return values;
    }

}
