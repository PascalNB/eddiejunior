package com.thefatrat.eddiejunior;

import com.thefatrat.eddiejunior.components.Component;
import com.thefatrat.eddiejunior.components.GlobalComponent;
import com.thefatrat.eddiejunior.events.*;
import com.thefatrat.eddiejunior.exceptions.BotException;
import com.thefatrat.eddiejunior.reply.*;
import com.thefatrat.eddiejunior.sources.Direct;
import com.thefatrat.eddiejunior.sources.Server;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.channel.update.ChannelUpdateArchivedEvent;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberUpdateEvent;
import net.dv8tion.jda.api.events.guild.scheduledevent.update.ScheduledEventUpdateStatusEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.utils.Result;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class Bot extends ListenerAdapter {

    private static Bot instance;

    private final Map<String, Server> servers = new HashMap<>();
    private final Direct direct = new Direct();
    private Class<? extends Component>[] components;
    private long time = 0;
    private JDA jda = null;
    private CommandManager commandManager = null;
    private File log = null;

    private Bot() {
    }

    public static Bot getInstance() {
        if (instance == null) {
            instance = new Bot();
        }
        return instance;
    }

    public void setJDA(JDA jda) {
        this.jda = jda;
        this.commandManager = new CommandManager(jda);
    }

    public JDA getJDA() {
        return jda;
    }

    public void setLog(File log) {
        this.log = log;
    }

    public File getLog() {
        return log;
    }

    public Server getServer(String id) {
        return servers.get(id);
    }

    @SafeVarargs
    public final void setComponents(Class<? extends Component>... components) {
        this.components = components;
    }

    private void loadServer(String id) {
        if (!servers.containsKey(id)) {
            Server server = new Server(id);
            servers.put(server.getId(), server);
            server.registerComponents(components);
            commandManager.setupGuildCommands(server);
        }
    }

    public String getUptime() {
        long t = System.currentTimeMillis() - time;
        long days = TimeUnit.MILLISECONDS.toDays(t);
        long hours = TimeUnit.MILLISECONDS.toHours(t);
        long min = TimeUnit.MILLISECONDS.toMinutes(t);
        long sec = TimeUnit.MILLISECONDS.toSeconds(t);
        return String.format("%d days, %d hours, %d minutes, %d seconds",
            days,
            hours - TimeUnit.DAYS.toHours(days),
            min - TimeUnit.HOURS.toMinutes(hours),
            sec - TimeUnit.MINUTES.toSeconds(min)
        );
    }

    public RestAction<List<Guild>> retrieveMutualGuilds(UserSnowflake user) {
        List<RestAction<Result<Member>>> actions = new ArrayList<>(jda.getGuilds().size());

        for (Guild guild : jda.getGuilds()) {
            actions.add(guild.retrieveMemberById(user.getId()).mapToResult());
        }

        return RestAction.allOf(actions)
            .map(list -> {
                List<Guild> mutualGuilds = new ArrayList<>();

                for (Result<Member> result : list) {
                    if (result.isSuccess()) {
                        mutualGuilds.add(result.get().getGuild());
                    }
                }

                return mutualGuilds;
            });
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        // noinspection unchecked
        Class<? extends Component>[] globalComponents = (Class<? extends Component>[]) Stream.of(components)
            .filter(c -> Arrays.asList(c.getInterfaces()).contains(GlobalComponent.class))
            .toArray(Class<?>[]::new);

        commandManager.setupGlobalCommands(globalComponents);
        jda.getPresence().setPresence(OnlineStatus.ONLINE, Activity.listening("✉️ DMs"));
        time = System.currentTimeMillis();
    }

    @Override
    public void onGuildReady(@NotNull GuildReadyEvent event) {
        String id = event.getGuild().getId();
        loadServer(id);
    }

    @Override
    public void onGuildJoin(@NotNull GuildJoinEvent event) {
        String id = event.getGuild().getId();
        loadServer(id);
    }

    @Override
    public void onGuildLeave(@NotNull GuildLeaveEvent event) {
        servers.remove(event.getGuild().getId());
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        if (event.getUser().isBot() || event.getUser().isSystem()) {
            return;
        }
        if (!event.isFromGuild()) {
            MenuReply reply = new MenuReply(event);

            try {
                ButtonEvent<User> bE = new ButtonEvent<>(event.getUser(), event.getComponentId(), event.getMessage());
                direct.getButtonHandler().handle(bE, reply);

            } catch (BotException e) {
                reply.edit(e);
            }
        } else {
            MenuReply reply = new MenuReply(event);

            try {
                Server server = getServer(Objects.requireNonNull(event.getGuild()).getId());

                Member member = Objects.requireNonNull(event.getMember());
                String buttonId = event.getComponentId();
                Message message = event.getMessage();

                server.getButtonHandler().handle(new ButtonEvent<>(member, buttonId, message), reply);
            } catch (BotException e) {
                reply.hide();
                reply.send(e);
            }
        }
    }

    @Override
    public void onStringSelectInteraction(@NotNull StringSelectInteractionEvent event) {
        if (event.getUser().isBot() || event.getUser().isSystem()) {
            return;
        }

        MenuReply reply = new MenuReply(event);
        SelectEvent<SelectOption> selectEvent = new SelectEvent<>(event.getUser(), event.getMessage(),
            event.getInteraction().getSelectedOptions().get(0));

        if (!event.isFromGuild()) {
            try {
                direct.getStringSelectHandler().handle(event.getComponentId(), selectEvent, reply);
            } catch (BotException e) {
                reply.edit(e);
            }
        } else {
            try {
                getServer(Objects.requireNonNull(event.getGuild()).getId()).getStringSelectHandler()
                    .handle(event.getComponentId(), selectEvent, reply);
            } catch (BotException e) {
                reply.hide();
                reply.send(e);
            }
        }

    }

    @Override
    public void onEntitySelectInteraction(@NotNull EntitySelectInteractionEvent event) {
        if (event.getUser().isBot() || event.getUser().isSystem()) {
            return;
        }
        if (event.isFromGuild()) {
            Guild guild = Objects.requireNonNull(event.getGuild());

            MenuReply reply = new MenuReply(event);

            try {
                SelectEvent<IMentionable> selectEvent = new SelectEvent<>(event.getUser(), event.getMessage(),
                    event.getInteraction().getValues().get(0));
                servers.get(guild.getId()).getEntitySelectHandler()
                    .handle(event.getComponentId(), selectEvent, reply);

            } catch (BotException e) {
                reply.edit(e);
            }
        }
    }

    @Override
    public void onMessageContextInteraction(@NotNull MessageContextInteractionEvent event) {
        if (event.getUser().isBot() || event.getUser().isSystem() || !event.isFromGuild()) {
            event.reply("Context interactions not allowed").queue();
            return;
        }

        Message message = event.getInteraction().getTarget();
        Guild guild = Objects.requireNonNull(event.getGuild());
        InteractionReply reply = new InteractionReply(event);

        try {
            String interaction = event.getName();
            Server server = servers.get(guild.getId());
            server.checkPermissions(Objects.requireNonNull(event.getMember()));
            server.getMessageInteractionHandler()
                .handle(interaction, new InteractionEvent<>(message, event.getMember()), reply);
        } catch (BotException e) {
            reply.hide();
            reply.send(e);
        }
    }

    @Override
    public void onUserContextInteraction(@NotNull UserContextInteractionEvent event) {
        if (event.getUser().isBot() || event.getUser().isSystem() || !event.isFromGuild()) {
            event.reply("Context interactions not allowed").queue();
            return;
        }

        Member member = event.getInteraction().getTargetMember();
        Guild guild = Objects.requireNonNull(event.getGuild());
        InteractionReply reply = new InteractionReply(event);

        try {
            String interaction = event.getName();
            Server server = servers.get(guild.getId());
            server.checkPermissions(Objects.requireNonNull(event.getMember()));
            server.getMemberInteractionHandler()
                .handle(interaction, new InteractionEvent<>(member, event.getMember()), reply);
        } catch (BotException e) {
            reply.hide();
            reply.send(e);
        }
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (event.getUser().isBot() || event.getUser().isSystem() || !event.isFromGuild()) {
            return;
        }

        Guild guild = event.getGuild();
        assert guild != null;

        Map<String, OptionMapping> options = new HashMap<>();
        for (OptionMapping option : event.getOptions()) {
            options.put(option.getName(), option);
        }

        InteractionReply reply = new InteractionReply(event);

        CommandEvent commandEvent = new CommandEvent(event.getName(), event.getSubcommandName(),
            options, event.getGuildChannel(), Objects.requireNonNull(event.getMember()));

        try {
            Server server = servers.get(guild.getId());
            server.checkPermissions(event.getMember());
            server.getCommandHandler().handle(event.getName(), commandEvent, reply);
        } catch (BotException e) {
            reply.hide();
            reply.send(e);
        }
    }

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        if (!event.isFromGuild()) {
            return;
        }

        Guild guild = Objects.requireNonNull(event.getGuild());
        Map<String, ModalMapping> map = new HashMap<>();

        for (ModalMapping value : event.getValues()) {
            map.put(value.getId(), value);
        }

        ModalEvent modalEvent = new ModalEvent(event.getMember(), map);
        DefaultReply reply = new DefaultReply(event);

        try {
            servers.get(guild.getId()).getModalHandler().handle(event.getModalId(), modalEvent, reply);
        } catch (BotException e) {
            reply.hide();
            reply.send(e);
        }
    }

    @Override
    public void onChannelUpdateArchived(@NotNull ChannelUpdateArchivedEvent event) {
        if (!event.isFromGuild() || (!event.isFromType(ChannelType.GUILD_PRIVATE_THREAD)
            && !event.isFromType(ChannelType.GUILD_PUBLIC_THREAD))) {
            return;
        }

        boolean archived = Boolean.TRUE.equals(event.getNewValue()) && !Boolean.TRUE.equals(event.getOldValue());
        Guild guild = Objects.requireNonNull(event.getGuild());
        ArchiveEvent archiveEvent = new ArchiveEvent(event.getChannel().asThreadChannel(), archived);

        servers.get(guild.getId()).getArchiveHandler().handle(archiveEvent, null);
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (event.isFromGuild() || event.getAuthor().isBot() || event.getAuthor().isSystem()) {
            return;
        }

        Message message = event.getMessage();
        Reply reply = new MessageReply(message);

        try {
            direct.receiveMessage(message, reply);
        } catch (BotException e) {
            reply.send(e);
        }
    }

    @Override
    public void onGuildMemberUpdate(@NotNull GuildMemberUpdateEvent event) {
        Guild guild = event.getGuild();
        GenericEvent<Member> genericEvent = new GenericEvent<>(event.getMember());
        servers.get(guild.getId()).<Member>getGenericHandler().handle("member", genericEvent, null);
    }

    @Override
    public void onGuildMemberJoin(@NotNull GuildMemberJoinEvent event) {
        Guild guild = event.getGuild();
        GenericEvent<Member> genericEvent = new GenericEvent<>(event.getMember());
        servers.get(guild.getId()).<Member>getGenericHandler().handle("member", genericEvent, null);
    }

    @Override
    public void onScheduledEventUpdateStatus(@NotNull ScheduledEventUpdateStatusEvent event) {
        if (event.getNewStatus().equals(event.getOldStatus())) {
            return;
        }

        EventEvent eventEvent = new EventEvent(event.getEntity().getName(), event.getEntity().getDescription(),
            event.getNewStatus(), event.getOldStatus());
        getServer(event.getGuild().getId()).getEventHandler().handle(eventEvent, null);
    }

    public RestAction<?> registerGuildCommands(String guildId, Component component) {
        return commandManager.registerGuildCommands(guildId, component);
    }

    public RestAction<?> deregisterGuildCommands(String guildId, Component component) {
        return commandManager.deregisterGuildCommands(guildId, component);
    }

}
