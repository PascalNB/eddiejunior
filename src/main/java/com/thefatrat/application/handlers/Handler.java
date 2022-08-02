package com.thefatrat.application.handlers;

import com.thefatrat.application.exceptions.BotException;
import com.thefatrat.application.util.Reply;

public interface Handler<T> {

    void handle(T t, Reply reply) throws BotException;

}
