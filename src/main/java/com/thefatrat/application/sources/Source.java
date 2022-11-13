package com.thefatrat.application.sources;

import com.thefatrat.application.entities.Reply;
import com.thefatrat.application.exceptions.BotException;
import net.dv8tion.jda.api.entities.Message;

public abstract class Source {

    public abstract void receiveMessage(Message message, Reply reply) throws BotException;

}
