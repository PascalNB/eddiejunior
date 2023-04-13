package com.thefatrat.eddiejunior.sources;

import com.thefatrat.eddiejunior.Bot;
import com.thefatrat.eddiejunior.CommandRegister;
import com.thefatrat.eddiejunior.HandlerCollection;
import com.thefatrat.eddiejunior.components.Component;
import com.thefatrat.eddiejunior.entities.Command;
import com.thefatrat.eddiejunior.entities.Interaction;
import com.thefatrat.eddiejunior.events.*;
import com.thefatrat.eddiejunior.handlers.ComponentHandler;
import com.thefatrat.eddiejunior.handlers.MapHandler;
import com.thefatrat.eddiejunior.handlers.SetHandler;
import com.thefatrat.eddiejunior.reply.EditReply;
import com.thefatrat.eddiejunior.reply.EphemeralReply;
import com.thefatrat.eddiejunior.reply.ModalReply;
import com.thefatrat.eddiejunior.reply.Reply;
import com.thefatrat.eddiejunior.util.Colors;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.internal.requests.CompletedRestAction;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.CheckReturnValue;
import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.util.*;

public class Server {

    private final String id;
    private final HandlerCollection<Member> handlerCollection = new HandlerCollection<>();
    private final ComponentHandler<?> directHandler = new ComponentHandler<>();
    private final Map<String, Component> components = new HashMap<>();
    private TextChannel log = null;

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

            for (Interaction<Message> interaction : component.getMessageInteractions()) {
                commandData.add(Commands.message(interaction.getName())
                    .setDefaultPermissions(DefaultMemberPermissions.enabledFor(interaction.getPermissions()))
                    .setGuildOnly(true));
            }

            for (Interaction<Member> interaction : component.getMemberInteractions()) {
                commandData.add(Commands.user(interaction.getName())
                    .setDefaultPermissions(DefaultMemberPermissions.enabledFor(interaction.getPermissions()))
                    .setGuildOnly(true));
            }

            return register.registerServerCommands(id, commandData);

        } else {
            List<RestAction<Void>> actions = new ArrayList<>();

            for (Command command : component.getCommands()) {
                actions.add(register.removeServerCommand(id, command.getName()));
            }

            for (Interaction<Message> interaction : component.getMessageInteractions()) {
                actions.add(register.removeServerCommand(id, interaction.getName()));
            }

            for (Interaction<Member> interaction : component.getMemberInteractions()) {
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
        set.addAll(handlerCollection.getMemberInteractionHandler().getKeys());
        return set;
    }

    public void setLog(TextChannel log) {
        this.log = log;
    }

    @Nullable
    public TextChannel getLog() {
        return log;
    }

    public void log(int color, User user, String message, Object... args) {
        if (log == null || !log.canTalk()) {
            return;
        }
        log.sendMessageEmbeds(new EmbedBuilder()
            .setColor(color)
            .setDescription(String.format("%s ", user.getAsMention()) +
                String.format(message, args))
            .setFooter(user.getId(), user.getEffectiveAvatarUrl())
            .setTimestamp(Instant.now())
            .build()
        ).queue();
    }

    public void log(User user, String message, Object... args) {
        log(Colors.TRANSPARENT, user, message, args);
    }

    public void log(int color, String message, Object... args) {
        if (log == null || !log.canTalk()) {
            return;
        }
        log.sendMessageEmbeds(new EmbedBuilder()
            .setColor(color)
            .setDescription(String.format(message, args))
            .setTimestamp(Instant.now())
            .build()
        ).queue();
    }

    public void log(String message, Object... args) {
        log(Colors.TRANSPARENT, message, args);
    }

    @SuppressWarnings("unchecked")
    public <T extends Reply & EditReply> ComponentHandler<T> getDirectMessageHandler() {
        return (ComponentHandler<T>) directHandler;
    }

    public <T extends Reply & EphemeralReply & ModalReply> MapHandler<CommandEvent, T> getCommandHandler() {
        return handlerCollection.getCommandHandler();
    }

    public <T extends Reply & EphemeralReply & ModalReply> MapHandler<InteractionEvent<Message>, T> getMessageInteractionHandler() {
        return handlerCollection.getMessageInteractionHandler();
    }

    public <T extends Reply & EphemeralReply & ModalReply> MapHandler<InteractionEvent<Member>, T> getMemberInteractionHandler() {
        return handlerCollection.getMemberInteractionHandler();
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

    public <T extends Reply & EphemeralReply & ModalReply & EditReply> MapHandler<SelectEvent<SelectOption>, T> getStringSelectHandler() {
        return handlerCollection.getStringSelectHandler();
    }

    public <T extends Reply & EphemeralReply & ModalReply & EditReply> MapHandler<SelectEvent<IMentionable>, T> getEntitySelectHandler() {
        return handlerCollection.getEntitySelectHandler();
    }

}
