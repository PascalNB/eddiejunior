package com.thefatrat.application.sources;

import com.thefatrat.application.Bot;
import com.thefatrat.application.CommandRegister;
import com.thefatrat.application.components.Component;
import com.thefatrat.application.entities.Command;
import com.thefatrat.application.entities.Interaction;
import com.thefatrat.application.entities.Reply;
import com.thefatrat.application.events.CommandEvent;
import com.thefatrat.application.events.InteractionEvent;
import com.thefatrat.application.exceptions.BotException;
import com.thefatrat.application.handlers.ArchiveHandler;
import com.thefatrat.application.handlers.CommandHandler;
import com.thefatrat.application.handlers.InteractionHandler;
import com.thefatrat.application.handlers.MessageHandler;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.RestAction;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Server extends Source {

    private final String id;
    private final CommandHandler commandHandler = new CommandHandler();
    private final InteractionHandler interactionHandler = new InteractionHandler();
    private final MessageHandler directHandler = new MessageHandler();
    private final ArchiveHandler archiveHandler = new ArchiveHandler();
    private final Map<String, Component> components = new HashMap<>();
    private final DefaultMemberPermissions permissions = DefaultMemberPermissions.enabledFor(
        Permission.USE_APPLICATION_COMMANDS
    );

    public static Server dummy() {
        return new Server();
    }

    private Server() {
        id = "";
    }

    public Server(String id) {
        this.id = id;
        CommandRegister.getInstance().retrieveServerCommands(id);
    }

    public Guild getGuild() {
        return Bot.getInstance().getJDA().getGuildById(id);
    }

    public String getId() {
        return id;
    }

    public MessageHandler getDirectHandler() {
        return directHandler;
    }

    public List<Component> getComponents() {
        List<Component> list = new ArrayList<>(components.values());
        list.sort((c1, c2) -> String.CASE_INSENSITIVE_ORDER.compare(c1.getName(), c2.getName()));
        return list;
    }

    public void toggleComponent(Component component, boolean enable) {
        if (enable) {

            List<CommandData> commandData = new ArrayList<>();

            for (Command command : component.getCommands()) {
                commandData.add(Commands.slash(command.getName(), command.getDescription())
                    .setDefaultPermissions(permissions)
                    .addOptions(command.getOptions())
                    .addSubcommands(command.getSubcommandsData()));
            }
            for (Interaction interaction : component.getInteractions()) {
                commandData.add(Commands.message(interaction.getName()).setGuildOnly(true));
            }

            CommandRegister.getInstance().registerServerCommands(id, commandData).queue();

            component.enable();
        } else {

            component.disable();

            List<RestAction<Void>> actions = new ArrayList<>();

            for (Command command : component.getCommands()) {
                actions.add(CommandRegister.getInstance().removeServerCommand(id, command.getName()));
            }

            for (Interaction interaction : component.getInteractions()) {
                actions.add(CommandRegister.getInstance().removeServerCommand(id, interaction.getName()));
            }

            RestAction.allOf(actions).complete();
        }
    }

    public Component getComponent(String componentName) {
        return components.get(componentName.toLowerCase());
    }

    @SafeVarargs
    public final void registerComponents(Class<? extends Component>... components) {
        try {
            for (Class<? extends Component> component : components) {
                Component instance = component.getDeclaredConstructor(Server.class).newInstance(this);
                instance.register();

                this.components.put(instance.getName(), instance);

                if (instance.isAlwaysEnabled()) {
                    continue;
                }

                if (instance.getDatabaseManager().isComponentEnabled()) {
                    toggleComponent(instance, true);
                }

            }

        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException |
                 IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public CommandHandler getCommandHandler() {
        return commandHandler;
    }

    public InteractionHandler getInteractionHandler() {
        return interactionHandler;
    }

    public ArchiveHandler getArchiveHandler() {
        return archiveHandler;
    }

    public void receiveInteraction(InteractionEvent message, Reply reply) {
        interactionHandler.handle(message, reply);
    }

    public void receiveCommand(CommandEvent event, Reply reply) {
        commandHandler.handle(event, reply);
    }

    @Override
    public void receiveMessage(Message message, Reply reply) throws BotException {
    }

}
