package com.thefatrat.application.components;

import com.thefatrat.application.sources.Source;

public abstract class Component {

    private final Source source;
    private final String name;
    private final boolean alwaysEnabled;
    private boolean enabled;

    public Component(Source source, String name, boolean alwaysEnabled) {
        this.name = name;
        this.source = source;
        this.alwaysEnabled = alwaysEnabled;
        enabled = alwaysEnabled;
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

    public abstract void register();

}
