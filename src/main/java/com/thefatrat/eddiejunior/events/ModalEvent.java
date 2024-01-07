package com.thefatrat.eddiejunior.events;

import com.thefatrat.eddiejunior.util.MetadataHolder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;

import java.util.Map;

public class ModalEvent implements MetadataHolder {

    private final String id;
    private final Member member;
    private final Map<String, ModalMapping> values;
    private Map<String, Object> metadata;

    public ModalEvent(String id, Member member, Map<String, ModalMapping> values) {
        this.id = id;
        this.member = member;
        this.values = values;
    }

    public Member getMember() {
        return member;
    }

    public Map<String, ModalMapping> getValues() {
        return values;
    }

    @Override
    public String getMetadataId() {
        return id;
    }

    @Override
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

}
