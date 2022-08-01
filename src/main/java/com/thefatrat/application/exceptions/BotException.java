package com.thefatrat.application.exceptions;

public abstract class BotException extends RuntimeException {

    public BotException(String message) {
        super(message);
    }

    @Override
    public String getMessage() {
        return getIcon() + " " + super.getMessage();
    }

    protected abstract String getIcon();

}
