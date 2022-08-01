package com.thefatrat.application.exceptions;

public class BotWarningException extends BotException {

    public static final String icon = ":warning:";

    public BotWarningException(String message) {
        super(message);
    }

    @Override
    protected String getIcon() {
        return icon;
    }

}
