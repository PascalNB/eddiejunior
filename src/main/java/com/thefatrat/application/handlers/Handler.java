package com.thefatrat.application.handlers;

import com.thefatrat.application.entities.Reply;

public interface Handler<T> {

    void handle(T t, Reply reply);

}
