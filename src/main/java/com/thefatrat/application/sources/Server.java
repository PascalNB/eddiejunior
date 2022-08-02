package com.thefatrat.application.sources;

import com.thefatrat.application.Bot;
import com.thefatrat.application.components.Component;
import com.thefatrat.application.components.DirectComponent;
import com.thefatrat.application.exceptions.BotException;
import com.thefatrat.application.handlers.CommandHandler;
import com.thefatrat.application.handlers.MessageHandler;
import com.thefatrat.application.util.Command;
import com.thefatrat.application.util.CommandEvent;
import com.thefatrat.application.util.Reply;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

public class Server extends Source {

    private final String id;
    private final CommandHandler commandHandler = new CommandHandler();
    private final MessageHandler directHandler = new MessageHandler();
    private final Map<String, Component> components = new HashMap<>();
    private final List<DirectComponent> directComponents = new ArrayList<>();

    public Server(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public MessageHandler getDirectHandler() {
        return directHandler;
    }

    public List<Component> getComponents() {
        return components.values().stream()
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
                Component instance = component.getDeclaredConstructor(Server.class)
                    .newInstance(this);
                instance.register();
                if (instance instanceof DirectComponent direct) {
                    directComponents.add(direct);
                }
                for (Command command : instance.getCommands()) {
                    Guild guild = Objects.requireNonNull(
                        Bot.getInstance().getJDA().getGuildById(id));
                    guild
                        .upsertCommand(command.getName(), command.getDescription())
                        .setDefaultPermissions(DefaultMemberPermissions.DISABLED)
                        .addOptions(command.getOptions())
                        .addSubcommands(command.getSubcommandsData())
                        .queue();
                }
                this.components.put(instance.getName(), instance);
            }
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException |
                 IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public CommandHandler getCommandHandler() {
        return commandHandler;
    }

    public void receiveCommand(CommandEvent event, Reply reply) {
        commandHandler.handle(event, reply);
    }

    @Override
    public void receiveMessage(Message message, Reply reply) throws BotException {
        getMessageHandler().handle(message, reply);
    }

}
