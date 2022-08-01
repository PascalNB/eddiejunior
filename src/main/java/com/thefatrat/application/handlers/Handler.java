package com.thefatrat.application.handlers;

import com.thefatrat.application.exceptions.BotException;

public interface Handler<T> {

    void handle(T t) throws BotException;

}
