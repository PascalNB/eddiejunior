package com.thefatrat.eddiejunior.exceptions;

import com.thefatrat.eddiejunior.util.Colors;
import com.thefatrat.eddiejunior.util.Icon;

public class BotErrorException extends BotException {

    public BotErrorException(String message) {
        super(message);
    }

    public BotErrorException(String message, Object... values) {
        super(message, values);
    }

    @Override
    public int getColor() {
        return Colors.RED;
    }

    @Override
    protected Icon getIcon() {
        return Icon.ERROR;
    }

}
