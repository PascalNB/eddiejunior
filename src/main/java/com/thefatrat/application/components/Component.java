package com.thefatrat.application.components;

import com.thefatrat.application.exceptions.BotErrorException;
import com.thefatrat.application.exceptions.BotException;
import com.thefatrat.application.sources.Source;
import net.dv8tion.jda.api.entities.MessageEmbed;

public abstract class Component {

    private final Source source;
    private final String title;
    private final boolean alwaysEnabled;
    private final int color;
    private boolean enabled;

    public Component(Source source, String title, boolean alwaysEnabled) {
        this.source = source;
        this.title = title;
        this.alwaysEnabled = alwaysEnabled;
        enabled = alwaysEnabled;
        color = getRandomColor(title);
    }

    protected void componentNotFound(String component) throws BotException {
        throw new BotErrorException(String.format("Component `%s` does not exist", component));
    }

    public boolean isAlwaysEnabled() {
        return alwaysEnabled;
    }

    public void enable() {
        enabled = true;
    }

    public void disable() {
        enabled = alwaysEnabled;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isEnabled() {
        return enabled;
    }

    public Source getSource() {
        return source;
    }

    public String getTitle() {
        return title;
    }

    public String getName() {
        return title.toLowerCase();
    }

    public abstract MessageEmbed getHelp();

    public abstract void register();

    public abstract String getStatus();

    public int getColor() {
        return color;
    }

    public static int getRandomColor(String seed) {
        int hash = 0;
        for (int i = 0; i < seed.length(); i++) {
            hash = seed.charAt(i) + ((hash << 5) - hash);
        }
        return hash & 0x00FFFFFF;
    }

}
