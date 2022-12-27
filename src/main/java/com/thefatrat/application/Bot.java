package com.thefatrat.application;

import com.thefatrat.application.components.Component;
import com.thefatrat.application.entities.Command;
import com.thefatrat.application.events.*;
import com.thefatrat.application.exceptions.BotException;
import com.thefatrat.application.reply.ComponentReply;
import com.thefatrat.application.reply.InteractionReply;
import com.thefatrat.application.reply.Reply;
import com.thefatrat.application.sources.Direct;
import com.thefatrat.application.sources.Server;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.channel.update.ChannelUpdateArchivedEvent;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
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
        CommandRegister.getInstance().setJDA(jda);
    }

    public JDA getJDA() {
        return jda;
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
        server.registerComponents(components);
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
        CommandRegister.getInstance().retrieveDefaultCommands();

        Server server = Server.dummy();
        server.registerComponents(components);
        List<Component> list = server.getComponents();
        List<Command> commands = new ArrayList<>();
        for (Component component : list) {
            if (component.isAlwaysEnabled()) {
                commands.addAll(component.getCommands());
            }
        }

        List<SlashCommandData> slashCommands = new ArrayList<>();
        for (Command command : commands) {
            SlashCommandData slashCommandData = Commands.slash(command.getName(), command.getDescription())
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.USE_APPLICATION_COMMANDS))
                .addOptions(command.getOptions())
                .addSubcommands(command.getSubcommandsData());
            slashCommands.add(slashCommandData);
        }
        CommandRegister.getInstance().registerDefaultCommands(slashCommands).complete();

        jda.getPresence().setPresence(OnlineStatus.ONLINE, Activity.playing("DM me to contact mods"));
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

            ComponentReply reply = new ComponentReply(event);

            try {
                ButtonEvent<User> bE = new ButtonEvent<>(event.getUser(), event.getComponentId(), event.getMessage());
                direct.getButtonHandler().handle(bE, reply);

            } catch (BotException e) {
                reply.getEditor().hide().except(e);
            }
        } else {
            ComponentReply reply = new ComponentReply(event);

            try {
                Server server = getServer(Objects.requireNonNull(event.getGuild()).getId());

                Member member = Objects.requireNonNull(event.getMember());
                String buttonId = event.getComponentId();
                Message message = event.getMessage();

                server.getButtonHandler().handle(new ButtonEvent<>(member, buttonId, message), reply);
            } catch (BotException e) {
                reply.getSender().hide().except(e);
            }
        }
    }

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        if (event.getUser().isBot() || event.getUser().isSystem()) {
            return;
        }
        if (!event.isFromGuild()) {
            ComponentReply reply = new ComponentReply(event);

            try {
                StringSelectEvent selectEvent = new StringSelectEvent(event.getUser(), event.getMessage(),
                    event.getComponentId(), event.getInteraction().getValues().get(0));
                direct.getStringSelectHandler().handle(selectEvent, reply);

            } catch (BotException e) {
                reply.getEditor().hide().except(e);
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
        Reply reply = new InteractionReply(event);

        try {
            String interaction = event.getName();
            servers.get(guild.getId()).getInteractionHandler().handleOne(interaction,
                new MessageInteractionEvent(message, interaction), reply);
        } catch (BotException e) {
            reply.hide().except(e);
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

        Reply reply = new InteractionReply(event);

        CommandEvent commandEvent = new CommandEvent(event.getName(), event.getSubcommandName(),
            options, guild, event.getGuildChannel(), Objects.requireNonNull(event.getMember()));

        try {
            servers.get(guild.getId()).getCommandHandler().handleOne(event.getName(), commandEvent, reply);
        } catch (BotException e) {
            reply.hide().except(e);
        }
    }

    @Override
    public void onChannelUpdateArchived(@NotNull ChannelUpdateArchivedEvent event) {
        if (!event.isFromGuild() && !event.isFromType(ChannelType.GUILD_PRIVATE_THREAD)
            && !event.isFromType(ChannelType.GUILD_PUBLIC_THREAD)) {
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
        Reply reply = Reply.defaultMessageReply(message);

        try {
            direct.receiveMessage(message, reply);
        } catch (BotException e) {
            reply.hide().except(e);
        }
    }

}
