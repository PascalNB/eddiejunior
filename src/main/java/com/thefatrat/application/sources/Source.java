package com.thefatrat.application.sources;

import com.thefatrat.application.components.Component;
import com.thefatrat.application.handlers.CommandHandler;
import com.thefatrat.application.handlers.Handler;
import com.thefatrat.application.handlers.MessageHandler;
import net.dv8tion.jda.api.entities.Message;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public abstract class Source {

    private final CommandHandler commandHandler = new CommandHandler();
    private final Handler<Message> messageHandler = new MessageHandler();
    private final Map<String, Component> components = new HashMap<>();

    public CommandHandler getCommandHandler() {
        return commandHandler;
    }

    public Handler<Message> getMessageHandler() {
        return messageHandler;
    }

    public boolean toggleComponent(String componentName, boolean enable) {
        Component component = components.get(componentName);
        if (component == null || component.isAlwaysEnabled()) {
            return false;
        }
        if (enable) {
            component.enable();
        } else {
            component.disable();
        }
        return true;
    }

    public Component getComponent(String componentName) {
        return components.get(componentName);
    }

    @SafeVarargs
    public final void registerComponents(Class<? extends Component>... components) {
        try {
            for (Class<? extends Component> component : components) {
                Component instance = component.getDeclaredConstructor(Source.class)
                    .newInstance(this);
                instance.register();
                this.components.put(instance.getName(), instance);
            }
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException |
                 IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public abstract void receiveMessage(Message message);

}
