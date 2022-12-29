package com.thefatrat.application.events;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;

import java.util.Map;

public class ModalEvent {

    public final Member member;
    public final String modalId;
    public final Map<String, ModalMapping> values;

    public ModalEvent(Member member, String modalId, Map<String, ModalMapping> values) {
        this.member = member;
        this.modalId = modalId;
        this.values = values;
    }

    public Member getMember() {
        return member;
    }

    public String getModalId() {
        return modalId;
    }

    public Map<String, ModalMapping> getValues() {
        return values;
    }

}
