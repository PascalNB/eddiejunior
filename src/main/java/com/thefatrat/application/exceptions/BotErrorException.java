package com.thefatrat.application.exceptions;

import com.thefatrat.application.util.Colors;

public class BotErrorException extends BotException {

    public static final String icon = ":x:";

    public BotErrorException(String message) {
        super(message);
    }

    @Override
    public int getColor() {
        return Colors.RED;
    }

    @Override
    protected String getIcon() {
        return icon;
    }

}
