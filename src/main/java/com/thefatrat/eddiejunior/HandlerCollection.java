package com.thefatrat.eddiejunior;

import com.thefatrat.eddiejunior.events.*;
import com.thefatrat.eddiejunior.handlers.MapHandler;
import com.thefatrat.eddiejunior.handlers.SetHandler;
import com.thefatrat.eddiejunior.reply.DefaultReply;
import com.thefatrat.eddiejunior.reply.InteractionReply;
import com.thefatrat.eddiejunior.reply.MenuReply;
import net.dv8tion.jda.api.entities.IMentionable;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;

public class HandlerCollection<V> {

    private MapHandler<CommandEvent, InteractionReply> commandHandler;
    private MapHandler<InteractionEvent<Message>, InteractionReply> messageInteractionHandler;
    private MapHandler<InteractionEvent<Member>, InteractionReply> memberInteractionHandler;
    private SetHandler<ArchiveEvent, Void> archiveHandler;
    private SetHandler<ButtonEvent<V>, MenuReply> buttonHandler;
    private MapHandler<ButtonEvent<V>, MenuReply> buttonMapHandler;
    private MapHandler<ModalEvent, DefaultReply> modalHandler;
    private SetHandler<EventEvent, Void> eventHandler;
    private MapHandler<SelectEvent<SelectOption>, MenuReply> stringSelectHandler;
    private MapHandler<SelectEvent<IMentionable>, MenuReply> entitySelectHandler;

    public MapHandler<CommandEvent, InteractionReply> getCommandHandler() {
        if (commandHandler == null) {
            commandHandler = new MapHandler<>();
        }
        return commandHandler;
    }

    public MapHandler<InteractionEvent<Message>, InteractionReply> getMessageInteractionHandler() {
        if (messageInteractionHandler == null) {
            messageInteractionHandler = new MapHandler<>();
        }
        return messageInteractionHandler;
    }

    public MapHandler<InteractionEvent<Member>, InteractionReply> getMemberInteractionHandler() {
        if (memberInteractionHandler == null) {
            memberInteractionHandler = new MapHandler<>();
        }
        return memberInteractionHandler;
    }

    public SetHandler<ArchiveEvent, Void> getArchiveHandler() {
        if (archiveHandler == null) {
            archiveHandler = new SetHandler<>();
        }
        return archiveHandler;
    }

    public SetHandler<ButtonEvent<V>, MenuReply> getButtonHandler() {
        if (buttonHandler == null) {
            buttonHandler = new SetHandler<>();
        }
        return buttonHandler;
    }

    public MapHandler<ButtonEvent<V>, MenuReply> getButtonMapHandler() {
        if (buttonMapHandler == null) {
            buttonMapHandler = new MapHandler<>();
        }
        return buttonMapHandler;
    }

    public MapHandler<ModalEvent, DefaultReply> getModalHandler() {
        if (modalHandler == null) {
            modalHandler = new MapHandler<>();
        }
        return modalHandler;
    }

    public SetHandler<EventEvent, Void> getEventHandler() {
        if (eventHandler == null) {
            eventHandler = new SetHandler<>();
        }
        return eventHandler;
    }

    public MapHandler<SelectEvent<SelectOption>, MenuReply> getStringSelectHandler() {
        if (stringSelectHandler == null) {
            stringSelectHandler = new MapHandler<>();
        }
        return stringSelectHandler;
    }

    public MapHandler<SelectEvent<IMentionable>, MenuReply> getEntitySelectHandler() {
        if (entitySelectHandler == null) {
            entitySelectHandler = new MapHandler<>();
        }
        return entitySelectHandler;
    }

}
