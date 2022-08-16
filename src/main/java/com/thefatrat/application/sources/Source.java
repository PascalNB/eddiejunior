package com.thefatrat.application.sources;

import com.thefatrat.application.entities.Reply;
import com.thefatrat.application.exceptions.BotException;
import com.thefatrat.application.handlers.Handler;
import com.thefatrat.application.handlers.MessageHandler;
import net.dv8tion.jda.api.entities.Message;

public abstract class Source {

    private final Handler<Message> messageHandler = new MessageHandler();

    public Handler<Message> getMessageHandler() {
        return messageHandler;
    }

    public abstract void receiveMessage(Message message, Reply reply)
    throws BotException;

}
