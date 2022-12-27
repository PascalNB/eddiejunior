package com.thefatrat.application.sources;

import com.thefatrat.application.Bot;
import com.thefatrat.application.CommandRegister;
import com.thefatrat.application.components.Component;
import com.thefatrat.application.entities.Command;
import com.thefatrat.application.entities.Interaction;
import com.thefatrat.application.events.ArchiveEvent;
import com.thefatrat.application.events.ButtonEvent;
import com.thefatrat.application.events.CommandEvent;
import com.thefatrat.application.events.MessageInteractionEvent;
import com.thefatrat.application.handlers.MapHandler;
import com.thefatrat.application.handlers.SetHandler;
import com.thefatrat.application.reply.ComponentReply;
import com.thefatrat.application.reply.Reply;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
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

public class Server {

    private static final DefaultMemberPermissions PERMISSIONS = DefaultMemberPermissions.enabledFor(
        Permission.USE_APPLICATION_COMMANDS
    );

    private final String id;
    private final MapHandler<CommandEvent, Reply> commandHandler = new MapHandler<>();
    private final MapHandler<MessageInteractionEvent, Reply> messageInteractionHandler = new MapHandler<>();
    private final MapHandler<Message, Reply> directHandler = new MapHandler<>();
    private final SetHandler<ArchiveEvent, Void> archiveHandler = new SetHandler<>();
    private final SetHandler<ButtonEvent<Member>, ComponentReply> buttonHandler = new SetHandler<>();
    private final Map<String, Component> components = new HashMap<>();

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

    public MapHandler<Message, Reply> getDirectHandler() {
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
                    .setDefaultPermissions(PERMISSIONS)
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

            RestAction.allOf(actions).queue();
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

    public MapHandler<CommandEvent, Reply> getCommandHandler() {
        return commandHandler;
    }

    public MapHandler<MessageInteractionEvent, Reply> getInteractionHandler() {
        return messageInteractionHandler;
    }

    public SetHandler<ArchiveEvent, Void> getArchiveHandler() {
        return archiveHandler;
    }

    public SetHandler<ButtonEvent<Member>, ComponentReply> getButtonHandler() {
        return buttonHandler;
    }

}
