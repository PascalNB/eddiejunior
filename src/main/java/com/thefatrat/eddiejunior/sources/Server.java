package com.thefatrat.eddiejunior.sources;

import com.thefatrat.eddiejunior.Bot;
import com.thefatrat.eddiejunior.DatabaseManager;
import com.thefatrat.eddiejunior.HandlerCollection;
import com.thefatrat.eddiejunior.RequestManager;
import com.thefatrat.eddiejunior.components.Component;
import com.thefatrat.eddiejunior.components.GlobalComponent;
import com.thefatrat.eddiejunior.entities.Command;
import com.thefatrat.eddiejunior.entities.Interaction;
import com.thefatrat.eddiejunior.entities.UserRole;
import com.thefatrat.eddiejunior.events.*;
import com.thefatrat.eddiejunior.exceptions.BotErrorException;
import com.thefatrat.eddiejunior.exceptions.BotException;
import com.thefatrat.eddiejunior.handlers.*;
import com.thefatrat.eddiejunior.reply.DefaultReply;
import com.thefatrat.eddiejunior.reply.InteractionReply;
import com.thefatrat.eddiejunior.reply.MenuReply;
import com.thefatrat.eddiejunior.util.Colors;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.requests.RestAction;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.CheckReturnValue;
import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class Server {

    private final String id;
    private final HandlerCollection<Member> handlerCollection = new HandlerCollection<>();
    private final ComponentHandler directHandler = new ComponentHandler();
    private final RequestHandler requestHandler = new RequestHandler();
    private final Map<String, Component> components = new HashMap<>();
    private final RequestManager requestManager = new RequestManager();
    private final Map<String, MapHandler<CommandEvent, InteractionReply>> subCommandHandler = new HashMap<>();
    private final AtomicReference<MessageChannel> log = new AtomicReference<>(null);
    private Role manageRole = null;
    private Role useRole = null;

    @NotNull
    @Contract(" -> new")
    public static Server dummy() {
        return new Server("");
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

    private void checkPermissions(@NotNull Member member, UserRole userRole) {
        if (userRole != null) {
            if (member.hasPermission(Permission.ADMINISTRATOR)) {
                return;
            }

            switch (userRole) {
                case MANAGE -> {
                    if (manageRole != null && member.getRoles().contains(manageRole)) {
                        return;
                    }
                }
                case USE -> {
                    if (manageRole != null && member.getRoles().contains(manageRole)
                        || useRole != null && member.getRoles().contains(useRole)) {
                        return;
                    }
                }
            }
        }

        throw new BotErrorException("No permission to interact");
    }

    public void checkCommandPermissions(@NotNull Member member, String command) throws BotException {
        checkPermissions(member, handlerCollection.getCommandHandler().getRequiredPermission(command));
    }

    public void checkMessageInteractionPermissions(@NotNull Member member, String interaction) throws BotException {
        checkPermissions(member, handlerCollection.getMessageInteractionHandler().getRequiredPermission(interaction));
    }

    public void checkMemberInteractionPermissions(@NotNull Member member, String interaction) throws BotException {
        checkPermissions(member, handlerCollection.getMemberInteractionHandler().getRequiredPermission(interaction));
    }

    public Role getManageRole() {
        return manageRole;
    }

    public Role getUseRole() {
        return useRole;
    }

    public void setManageRole(@Nullable Role manageRole) {
        this.manageRole = manageRole;
    }

    public void setUseRole(@Nullable Role useRole) {
        this.useRole = useRole;
    }

    public List<Component> getComponents() {
        List<Component> list = new ArrayList<>(components.values());
        list.sort((c1, c2) -> String.CASE_INSENSITIVE_ORDER.compare(c1.getId(), c2.getId()));
        return list;
    }

    @CheckReturnValue
    public RestAction<?> toggleComponent(Component component, boolean enable) {
        if (enable) {
            return Bot.getInstance().registerGuildCommands(id, component);
        } else {
            return Bot.getInstance().deregisterGuildCommands(id, component);
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
    public final Collection<Component> registerComponents(Class<? extends Component> @NotNull ... components) {
        for (Class<? extends Component> component : components) {
            try {
                Component instance = component.getDeclaredConstructor(Server.class).newInstance(this);

                for (Command command : instance.getCommands()) {
                    UserRole minPermission = command.getRequiredUserRole() == null
                        ? UserRole.USE
                        : command.getRequiredUserRole();

                    if (command.hasSubCommands()) {
                        command.setAction((c, reply) -> {
                            CommandEvent event = c.toSub();
                            subCommandHandler.get(command.getName()).handle(event.getName(), event, reply);
                        });

                        MapHandler<CommandEvent, InteractionReply> mapHandler = new MapHandler<>();
                        subCommandHandler.put(command.getName(), mapHandler);
                        for (Command sub : command.getSubcommands()) {
                            mapHandler.addListener(sub.getName(), sub.getAction());

                            String commandName = command.getName() + " " + sub.getName();
                            if (sub.getRequiredUserRole() != null) {
                                getCommandHandler().addRequiredPermission(commandName, sub.getRequiredUserRole());
                            } else {
                                getCommandHandler().addRequiredPermission(commandName, minPermission);
                            }
                        }
                    } else {
                        getCommandHandler().addRequiredPermission(command.getName(), minPermission);
                    }

                    getCommandHandler().addListener(command.getName(), command.getAction());
                }

                for (Interaction<Message> interaction : instance.getMessageInteractions()) {
                    getMessageInteractionHandler().addListener(interaction.getName(), interaction.getAction());
                    UserRole permission = interaction.getRequiredUserRole() == null
                        ? UserRole.USE : interaction.getRequiredUserRole();
                    getMessageInteractionHandler().addRequiredPermission(interaction.getName(), permission);
                }

                for (Interaction<Member> interaction : instance.getMemberInteractions()) {
                    getMemberInteractionHandler().addListener(interaction.getName(), interaction.getAction());
                    UserRole permission = interaction.getRequiredUserRole() == null
                        ? UserRole.USE : interaction.getRequiredUserRole();
                    getMemberInteractionHandler().addRequiredPermission(interaction.getName(), permission);
                }

                this.components.put(instance.getId(), instance);

                if (instance instanceof GlobalComponent) {
                    instance.enable();
                    continue;
                }

                if (DatabaseManager.isComponentEnabled(id, instance.getId())) {
                    instance.enable();
                    toggleComponent(instance, true).queue();
                }

            } catch (NoSuchMethodException | InvocationTargetException | InstantiationException |
                     IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        return this.components.values();
    }

    public Set<String> getRegisteredCommands() {
        Set<String> set = new HashSet<>();
        set.addAll(handlerCollection.getCommandHandler().getKeys());
        set.addAll(handlerCollection.getMessageInteractionHandler().getKeys());
        set.addAll(handlerCollection.getMemberInteractionHandler().getKeys());
        return set;
    }

    public void setLog(MessageChannel log) {
        this.log.set(log);
    }

    @Nullable
    public MessageChannel getLog() {
        return log.get();
    }

    public void log(int color, User user, String message, Object... args) {
        MessageChannel log = this.log.get();
        if (log == null || !log.canTalk()) {
            return;
        }
        EmbedBuilder builder = new EmbedBuilder()
            .setColor(color)
            .setTimestamp(Instant.now());

        String description = String.format(message, args);
        if (user != null) {
            description = user.getAsMention() + ": " + description;
            builder.setFooter(user.getId(), user.getEffectiveAvatarUrl());
        }

        builder.setDescription(description);

        log.sendMessageEmbeds(builder.build()).queue();
    }

    public void log(User user, String message, Object... args) {
        log(Colors.TRANSPARENT, user, message, args);
    }

    public void log(int color, String message, Object... args) {
        log(color, null, message, args);
    }

    public void log(String message, Object... args) {
        log(Colors.TRANSPARENT, null, message, args);
    }

    public RequestManager getRequestManager() {
        return requestManager;
    }

    public ComponentHandler getDirectMessageHandler() {
        return directHandler;
    }

    public RequestHandler getRequestHandler() {
        return requestHandler;
    }

    public PermissionMapHandler<CommandEvent, InteractionReply> getCommandHandler() {
        return handlerCollection.getCommandHandler();
    }

    public PermissionMapHandler<InteractionEvent<Message>, InteractionReply> getMessageInteractionHandler() {
        return handlerCollection.getMessageInteractionHandler();
    }

    public PermissionMapHandler<InteractionEvent<Member>, InteractionReply> getMemberInteractionHandler() {
        return handlerCollection.getMemberInteractionHandler();
    }

    public SetHandler<ArchiveEvent, Void> getArchiveHandler() {
        return handlerCollection.getArchiveHandler();
    }

    public SetHandler<ButtonEvent<Member>, MenuReply> getButtonHandler() {
        return handlerCollection.getButtonHandler();
    }

    public MapHandler<ModalEvent, DefaultReply> getModalHandler() {
        return handlerCollection.getModalHandler();
    }

    public SetHandler<EventEvent, Void> getEventHandler() {
        return handlerCollection.getEventHandler();
    }

    public MapHandler<SelectEvent<SelectOption>, MenuReply> getStringSelectHandler() {
        return handlerCollection.getStringSelectHandler();
    }

    public MapHandler<SelectEvent<IMentionable>, MenuReply> getEntitySelectHandler() {
        return handlerCollection.getEntitySelectHandler();
    }

    public <T> MapHandler<GenericEvent<T>, Void> getGenericHandler() {
        return handlerCollection.getGenericHandler();
    }

}
