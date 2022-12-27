package com.thefatrat.application.handlers;

public interface Handler<T, R> {

    void handle(T t, R reply);

}
