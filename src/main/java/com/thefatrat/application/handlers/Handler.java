package com.thefatrat.application.handlers;

public interface Handler<T> {
    
    boolean handle(T t);

}
