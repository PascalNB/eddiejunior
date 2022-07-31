package com.thefatrat.application.sources;

import com.thefatrat.application.components.Component;
import com.thefatrat.application.components.DirectComponent;
import com.thefatrat.application.handlers.CommandHandler;
import com.thefatrat.application.handlers.Handler;
import com.thefatrat.application.handlers.MessageHandler;
import net.dv8tion.jda.api.entities.Message;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class Source {

    private final CommandHandler commandHandler = new CommandHandler();
    private final Handler<Message> messageHandler = new MessageHandler();
    private final Map<String, Component> components = new HashMap<>();
    private final List<DirectComponent> directComponents = new ArrayList<>();

    public CommandHandler getCommandHandler() {
        return commandHandler;
    }

    public Handler<Message> getMessageHandler() {
        return messageHandler;
    }

    public List<Component> getComponents() {
        return components.values().stream()
            .filter(Component::isEnabled)
            .sorted((c1, c2) -> String.CASE_INSENSITIVE_ORDER.compare(c1.getName(), c2.getName()))
            .collect(Collectors.toList());
    }

    public List<DirectComponent> getDirectComponents() {
        return directComponents;
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
        return components.get(componentName.toLowerCase());
    }

    @SafeVarargs
    public final void registerComponents(Class<? extends Component>... components) {
        try {
            for (Class<? extends Component> component : components) {
                Component instance = component.getDeclaredConstructor(Source.class)
                    .newInstance(this);
                instance.register();
                if (instance instanceof DirectComponent direct) {
                    directComponents.add(direct);
                }
                this.components.put(instance.getName(), instance);
            }
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException |
                 IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public abstract void receiveMessage(Message message);

}
