package com.thefatrat.eddiejunior.events;

public class GenericEvent<T> {

    private final T t;

    public GenericEvent(T t) {
        this.t = t;
    }

    public T getEntity() {
        return t;
    }

}
