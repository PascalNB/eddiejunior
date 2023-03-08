package com.thefatrat.eddiejunior;

import com.thefatrat.eddiejunior.components.Component;
import com.thefatrat.eddiejunior.entities.Command;
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
import net.dv8tion.jda.api.events.guild.scheduledevent.update.ScheduledEventUpdateStatusEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.utils.Result;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class Bot extends ListenerAdapter {

    private static Bot instance;

    private final Map<String, Server> servers = new HashMap<>();
    private final Direct direct = new Direct();
    private Class<? extends Component>[] components;
    private long time = 0;
    private JDA jda = null;
    private CommandRegister commandRegister = null;

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
        this.commandRegister = new CommandRegister(jda);
    }

    public JDA getJDA() {
        return jda;
    }

    public CommandRegister getCommandRegister() {
        return commandRegister;
    }

    public Server getServer(String id) {
        return servers.get(id);
    }

    @SafeVarargs
    public final void setComponents(Class<? extends Component>... components) {
        this.components = components;
    }

    private void loadServer(String id) {
        Server server = new Server(id);
        servers.put(server.getId(), server);
        commandRegister.retrieveServerCommands(id).complete();
        server.registerComponents(components);
        commandRegister.filterServerCommands(id, server.getRegisteredCommands()).queue();
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
        commandRegister.retrieveDefaultCommands().complete();

        Server server = Server.dummy();
        Collection<Component> list = server.registerComponents(components);
        List<Command> commands = new ArrayList<>();
        for (Component component : list) {
            if (component.isGlobalComponent()) {
                commands.addAll(component.getCommands());
            }
        }

        Set<String> commandNames = new HashSet<>();
        List<SlashCommandData> slashCommands = new ArrayList<>();
        for (Command command : commands) {
            SlashCommandData slashCommandData = Commands.slash(command.getName(), command.getDescription())
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(command.getPermissions()))
                .addOptions(command.getOptions())
                .addSubcommands(command.getSubcommandsData());
            slashCommands.add(slashCommandData);
            commandNames.add(command.getName());
        }
        commandRegister.registerDefaultCommands(slashCommands).complete();
        commandRegister.filterDefaultCommands(commandNames).queue();

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
            ComponentReply<ButtonInteractionEvent> reply = new ComponentReply<>(event);

            try {
                ButtonEvent<User> bE = new ButtonEvent<>(event.getUser(), event.getComponentId(), event.getMessage());
                direct.getButtonHandler().handle(event.getComponentId(), bE, reply);

            } catch (BotException e) {
                reply.edit(e);
            }
        } else {
            ComponentReply<ButtonInteractionEvent> reply = new ComponentReply<>(event);

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
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        if (event.getUser().isBot() || event.getUser().isSystem()) {
            return;
        }
        if (!event.isFromGuild()) {
            ComponentReply<StringSelectInteractionEvent> reply = new ComponentReply<>(event);

            try {
                StringSelectEvent selectEvent = new StringSelectEvent(event.getUser(), event.getMessage(),
                    event.getComponentId(), event.getInteraction().getValues().get(0));
                direct.getStringSelectHandler().handle(event.getComponentId(), selectEvent, reply);

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
        InteractionReply<MessageContextInteractionEvent> reply = new InteractionReply<>(event);

        try {
            String interaction = event.getName();
            servers.get(guild.getId()).getMessageInteractionHandler().handle(interaction,
                new InteractionEvent<>(message, event.getMember(), interaction), reply);
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
        InteractionReply<UserContextInteractionEvent> reply = new InteractionReply<>(event);

        try {
            String interaction = event.getName();
            servers.get(guild.getId()).getMemberInteractionHandler().handle(interaction,
                new InteractionEvent<>(member, event.getMember(), interaction), reply);
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

        InteractionReply<SlashCommandInteractionEvent> reply = new InteractionReply<>(event);

        CommandEvent commandEvent = new CommandEvent(event.getName(), event.getSubcommandName(),
            options, event.getGuildChannel(), Objects.requireNonNull(event.getMember()));

        try {
            servers.get(guild.getId()).getCommandHandler().handle(event.getName(), commandEvent, reply);
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

        ModalEvent modalEvent = new ModalEvent(event.getMember(), event.getModalId(), map);
        GenericReply reply = new GenericReply(event);

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
        if (!archived) {
            return;
        }
        Guild guild = Objects.requireNonNull(event.getGuild());
        ArchiveEvent archiveEvent = new ArchiveEvent(event.getChannel().asThreadChannel());

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
    public void onScheduledEventUpdateStatus(@NotNull ScheduledEventUpdateStatusEvent event) {
        if (event.getNewStatus().equals(event.getOldStatus())) {
            return;
        }

        EventEvent eventEvent = new EventEvent(event.getEntity().getName(), event.getEntity().getDescription(),
            event.getNewStatus());
        getServer(event.getGuild().getId()).getEventHandler().handle(eventEvent, null);
    }

}
