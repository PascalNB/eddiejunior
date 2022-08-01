package com.thefatrat.application.components;

import com.thefatrat.application.exceptions.BotErrorException;
import com.thefatrat.application.exceptions.BotException;
import com.thefatrat.application.sources.Source;
import net.dv8tion.jda.api.entities.MessageEmbed;

public abstract class Component {

    private final Source source;
    private final String name;
    private final boolean alwaysEnabled;
    private boolean enabled;

    public Component(Source source, String name, boolean alwaysEnabled) {
        this.name = name.toLowerCase();
        this.source = source;
        this.alwaysEnabled = alwaysEnabled;
        enabled = alwaysEnabled;
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

    public String getName() {
        return name;
    }

    public abstract MessageEmbed getHelp();

    public abstract void register();

}
