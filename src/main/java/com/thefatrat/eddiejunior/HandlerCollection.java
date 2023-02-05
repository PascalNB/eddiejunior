package com.thefatrat.eddiejunior;

import com.thefatrat.eddiejunior.events.*;
import com.thefatrat.eddiejunior.handlers.MapHandler;
import com.thefatrat.eddiejunior.handlers.SetHandler;
import com.thefatrat.eddiejunior.reply.EditReply;
import com.thefatrat.eddiejunior.reply.EphemeralReply;
import com.thefatrat.eddiejunior.reply.ModalReply;
import com.thefatrat.eddiejunior.reply.Reply;

@SuppressWarnings("unchecked")
public class HandlerCollection<V> {

    private MapHandler<CommandEvent, ?> commandHandler;
    private MapHandler<MessageInteractionEvent, ?> messageInteractionHandler;
    private SetHandler<ArchiveEvent, Void> archiveHandler;
    private SetHandler<ButtonEvent<V>, ?> buttonHandler;
    private MapHandler<ButtonEvent<V>, ?> buttonMapHandler;
    private MapHandler<ModalEvent, ?> modalHandler;
    private SetHandler<EventEvent, Void> eventHandler;
    private MapHandler<StringSelectEvent, ?> stringSelectHandler;

    public <T extends Reply & EphemeralReply & ModalReply> MapHandler<CommandEvent, T> getCommandHandler() {
        if (commandHandler == null) {
            commandHandler = new MapHandler<>();
        }
        return (MapHandler<CommandEvent, T>) commandHandler;
    }

    public <T extends Reply & EphemeralReply & ModalReply> MapHandler<MessageInteractionEvent, T> getMessageInteractionHandler() {
        if (messageInteractionHandler == null) {
            messageInteractionHandler = new MapHandler<>();
        }
        return (MapHandler<MessageInteractionEvent, T>) messageInteractionHandler;
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

    public <T extends Reply & EphemeralReply & EditReply> MapHandler<StringSelectEvent, T> getStringSelectHandler() {
        if (stringSelectHandler == null) {
            stringSelectHandler = new MapHandler<>();
        }
        return (MapHandler<StringSelectEvent, T>) stringSelectHandler;
    }

}
