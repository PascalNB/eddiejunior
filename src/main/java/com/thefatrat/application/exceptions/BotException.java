package com.thefatrat.application.exceptions;

public abstract class BotException extends RuntimeException {

    public BotException(String message) {
        super(message);
    }

    @Override
    public String getMessage() {
        return getIcon() + " " + super.getMessage();
    }

    public abstract int getColor();

    protected abstract String getIcon();

}
