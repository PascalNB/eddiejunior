package com.thefatrat.eddiejunior.exceptions;

import com.thefatrat.eddiejunior.util.Colors;
import com.thefatrat.eddiejunior.util.Icon;

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
    protected Icon getIcon() {
        return Icon.WARNING;
    }

}
