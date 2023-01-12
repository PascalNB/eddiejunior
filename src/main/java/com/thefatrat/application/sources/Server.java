package com.thefatrat.application.sources;

import com.thefatrat.application.Bot;
import com.thefatrat.application.CommandRegister;
import com.thefatrat.application.HandlerCollection;
import com.thefatrat.application.components.Component;
import com.thefatrat.application.entities.Command;
import com.thefatrat.application.entities.Interaction;
import com.thefatrat.application.events.*;
import com.thefatrat.application.handlers.MapHandler;
import com.thefatrat.application.handlers.SetHandler;
import com.thefatrat.application.reply.EditReply;
import com.thefatrat.application.reply.EphemeralReply;
import com.thefatrat.application.reply.ModalReply;
import com.thefatrat.application.reply.Reply;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.internal.requests.CompletedRestAction;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.CheckReturnValue;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class Server {

    private final String id;
    private final HandlerCollection<Member> handlerCollection = new HandlerCollection<>();
    private final Map<String, Component> components = new HashMap<>();

    @NotNull
    @Contract(" -> new")
    public static Server dummy() {
        return new Server();
    }

    private Server() {
        id = "";
    }

    public Server(String id) {
        this.id = id;
    }

    public Guild getGuild() {
        return Bot.getInstance().getJDA().getGuildById(id);
    }

    public String getId() {
        return id;
    }

    public List<Component> getComponents() {
        List<Component> list = new ArrayList<>(components.values());
        list.sort((c1, c2) -> String.CASE_INSENSITIVE_ORDER.compare(c1.getName(), c2.getName()));
        return list;
    }

    @CheckReturnValue
    public RestAction<?> toggleComponent(Component component, boolean enable) {
        CommandRegister register = Bot.getInstance().getCommandRegister();
        if (enable) {

            List<CommandData> commandData = new ArrayList<>();

            for (Command command : component.getCommands()) {
                commandData.add(Commands.slash(command.getName(), command.getDescription())
                    .setDefaultPermissions(DefaultMemberPermissions.enabledFor(command.getPermissions()))
                    .addOptions(command.getOptions())
                    .addSubcommands(command.getSubcommandsData()));
            }
            for (Interaction interaction : component.getInteractions()) {
                commandData.add(Commands.message(interaction.getName()).setGuildOnly(true));
            }

            return register.registerServerCommands(id, commandData);

        } else {
            List<RestAction<Void>> actions = new ArrayList<>();

            for (Command command : component.getCommands()) {
                actions.add(register.removeServerCommand(id, command.getName()));
            }

            for (Interaction interaction : component.getInteractions()) {
                actions.add(register.removeServerCommand(id, interaction.getName()));
            }

            if (actions.isEmpty()) {
                return new CompletedRestAction<>(getGuild().getJDA(), (Object) null);
            }

            return RestAction.allOf(actions);
        }
    }

    @Nullable
    public Component getComponent(@NotNull String componentName) {
        return components.get(componentName.toLowerCase());
    }

    @Nullable
    public <T extends Component> T getComponent(String componentName, @NotNull Class<T> clazz) {
        return clazz.cast(getComponent(componentName));
    }

    @NotNull
    @SafeVarargs
    public final Collection<Component> registerComponents(@NotNull Class<? extends Component>... components) {
        try {
            for (Class<? extends Component> component : components) {
                Component instance = component.getDeclaredConstructor(Server.class).newInstance(this);
                instance.register();

                this.components.put(instance.getName(), instance);

                if (instance.isGlobalComponent()) {
                    continue;
                }

                if (instance.getDatabaseManager().isComponentEnabled()) {
                    instance.enable();
                    toggleComponent(instance, true).queue();
                }

            }

            return this.components.values();
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException |
                 IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public Set<String> getRegisteredCommands() {
        Set<String> set = new HashSet<>();
        set.addAll(handlerCollection.getCommandHandler().getKeys());
        set.addAll(handlerCollection.getMessageInteractionHandler().getKeys());
        return set;
    }

    public <T extends Reply> MapHandler<Message, T> getDirectMessageHandler() {
        return handlerCollection.getMessageHandler();
    }

    public <T extends Reply & EphemeralReply & ModalReply> MapHandler<CommandEvent, T> getCommandHandler() {
        return handlerCollection.getCommandHandler();
    }

    public <T extends Reply & EphemeralReply & ModalReply> MapHandler<MessageInteractionEvent, T> getInteractionHandler() {
        return handlerCollection.getMessageInteractionHandler();
    }

    public SetHandler<ArchiveEvent, Void> getArchiveHandler() {
        return handlerCollection.getArchiveHandler();
    }

    public <T extends Reply & EphemeralReply & EditReply & ModalReply>
    SetHandler<ButtonEvent<Member>, T> getButtonHandler() {
        return handlerCollection.getButtonHandler();
    }

    public <T extends Reply & EphemeralReply> MapHandler<ModalEvent, T> getModalHandler() {
        return handlerCollection.getModalHandler();
    }

    public SetHandler<EventEvent, Void> getEventHandler() {
        return handlerCollection.getEventHandler();
    }

}
