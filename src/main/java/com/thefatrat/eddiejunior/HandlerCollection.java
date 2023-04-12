package com.thefatrat.eddiejunior;

import com.thefatrat.eddiejunior.events.*;
import com.thefatrat.eddiejunior.handlers.MapHandler;
import com.thefatrat.eddiejunior.handlers.SetHandler;
import com.thefatrat.eddiejunior.reply.EditReply;
import com.thefatrat.eddiejunior.reply.EphemeralReply;
import com.thefatrat.eddiejunior.reply.ModalReply;
import com.thefatrat.eddiejunior.reply.Reply;
import net.dv8tion.jda.api.entities.IMentionable;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;

@SuppressWarnings("unchecked")
public class HandlerCollection<V> {

    private MapHandler<CommandEvent, ?> commandHandler;
    private MapHandler<InteractionEvent<Message>, ?> messageInteractionHandler;
    private MapHandler<InteractionEvent<Member>, ?> memberInteractionHandler;
    private SetHandler<ArchiveEvent, Void> archiveHandler;
    private SetHandler<ButtonEvent<V>, ?> buttonHandler;
    private MapHandler<ButtonEvent<V>, ?> buttonMapHandler;
    private MapHandler<ModalEvent, ?> modalHandler;
    private SetHandler<EventEvent, Void> eventHandler;
    private MapHandler<SelectEvent<String>, ?> stringSelectHandler;
    private MapHandler<SelectEvent<IMentionable>, ?> entitySelectHandler;

    public <T extends Reply & EphemeralReply & ModalReply> MapHandler<CommandEvent, T> getCommandHandler() {
        if (commandHandler == null) {
            commandHandler = new MapHandler<>();
        }
        return (MapHandler<CommandEvent, T>) commandHandler;
    }

    public <T extends Reply & EphemeralReply & ModalReply> MapHandler<InteractionEvent<Message>, T> getMessageInteractionHandler() {
        if (messageInteractionHandler == null) {
            messageInteractionHandler = new MapHandler<>();
        }
        return (MapHandler<InteractionEvent<Message>, T>) messageInteractionHandler;
    }

    public <T extends Reply & EphemeralReply & ModalReply> MapHandler<InteractionEvent<Member>, T> getMemberInteractionHandler() {
        if (memberInteractionHandler == null) {
            memberInteractionHandler = new MapHandler<>();
        }
        return (MapHandler<InteractionEvent<Member>, T>) memberInteractionHandler;
    }

    public SetHandler<ArchiveEvent, Void> getArchiveHandler() {
        if (archiveHandler == null) {
            archiveHandler = new SetHandler<>();
        }
        return archiveHandler;
    }

    public <T extends Reply & EphemeralReply & EditReply & ModalReply>
    SetHandler<ButtonEvent<V>, T> getButtonHandler() {
        if (buttonHandler == null) {
            buttonHandler = new SetHandler<>();
        }
        return (SetHandler<ButtonEvent<V>, T>) buttonHandler;
    }

    public <T extends Reply & EphemeralReply & EditReply & ModalReply> MapHandler<ButtonEvent<V>, T> getButtonMapHandler() {
        if (buttonMapHandler == null) {
            buttonMapHandler = new MapHandler<>();
        }
        return (MapHandler<ButtonEvent<V>, T>) buttonMapHandler;
    }

    public <T extends Reply & EphemeralReply> MapHandler<ModalEvent, T> getModalHandler() {
        if (modalHandler == null) {
            modalHandler = new MapHandler<>();
        }
        return (MapHandler<ModalEvent, T>) modalHandler;
    }

    public SetHandler<EventEvent, Void> getEventHandler() {
        if (eventHandler == null) {
            eventHandler = new SetHandler<>();
        }
        return eventHandler;
    }

    public <T extends Reply & EphemeralReply & EditReply> MapHandler<SelectEvent<String>, T> getStringSelectHandler() {
        if (stringSelectHandler == null) {
            stringSelectHandler = new MapHandler<>();
        }
        return (MapHandler<SelectEvent<String>, T>) stringSelectHandler;
    }

    public <T extends Reply & EphemeralReply & EditReply & ModalReply> MapHandler<SelectEvent<IMentionable>, T> getEntitySelectHandler() {
        if (entitySelectHandler == null) {
            entitySelectHandler = new MapHandler<>();
        }
        return (MapHandler<SelectEvent<IMentionable>, T>) entitySelectHandler;
    }

}
