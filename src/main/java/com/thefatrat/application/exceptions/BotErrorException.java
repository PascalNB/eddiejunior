package com.thefatrat.application.exceptions;

public class BotErrorException extends BotException {

    public static final String icon = ":x:";

    public BotErrorException(String message) {
        super(message);
    }

    @Override
    protected String getIcon() {
        return icon;
    }

}
