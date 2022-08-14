package com.thefatrat.application.exceptions;

import com.thefatrat.application.util.Colors;

public class BotWarningException extends BotException {

    public static final String icon = ":warning:";

    public BotWarningException(String message) {
        super(message);
    }

    @Override
    public int getColor() {
        return Colors.YELLOW;
    }

    @Override
    protected String getIcon() {
        return icon;
    }

}
