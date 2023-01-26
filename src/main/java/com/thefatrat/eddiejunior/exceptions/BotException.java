package com.thefatrat.eddiejunior.exceptions;

import com.thefatrat.eddiejunior.util.Icon;

public abstract class BotException extends RuntimeException {

    public BotException(String message) {
        super(message);
    }

    public BotException(String message, Object... values) {
        super(String.format(message, values));
    }

    @Override
    public String getMessage() {
        return getIcon() + " " + super.getMessage();
    }

    public abstract int getColor();

    protected abstract Icon getIcon();

}
