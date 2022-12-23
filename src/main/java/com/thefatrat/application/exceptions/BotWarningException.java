package com.thefatrat.application.exceptions;

import com.thefatrat.application.util.Colors;
import com.thefatrat.application.util.Icons;

public class BotWarningException extends BotException {

    public BotWarningException(String message) {
        super(message);
    }

    public BotWarningException(String message, Object... values) {
        super(message, values);
    }

    @Override
    public int getColor() {
        return Colors.YELLOW;
    }

    @Override
    protected String getIcon() {
        return Icons.WARNING;
    }

}
