package com.thefatrat.eddiejunior.util;

public enum Icon {
    OK(":white_check_mark:", Colors.GREEN),
    ERROR(":x:", Colors.RED),
    WARNING(":warning:", Colors.YELLOW),
    SETTING(":gear:", Colors.GRAY),
    STOP(":stop_sign:", Colors.WHITE),
    ENABLE(":ballot_box_with_check:", Colors.BLUE),
    DISABLE(":no_entry:", Colors.WHITE),
    RESET(":arrows_counterclockwise:", Colors.WHITE),
    WIN(":trophy:", Colors.GOLD),
    PAUSE(":pause_button:", Colors.WHITE);

    private final String icon;
    private final int color;

    Icon(String icon, int color) {
        this.icon = icon;
        this.color = color;
    }

    public int getColor() {
        return color;
    }

    @Override
    public String toString() {
        return icon;
    }
}
