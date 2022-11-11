package com.thefatrat.application;

import com.thefatrat.application.components.Component;
import com.thefatrat.application.entities.Reply;
import com.thefatrat.application.events.ArchiveEvent;
import com.thefatrat.application.events.CommandEvent;
import com.thefatrat.application.events.InteractionEvent;
import com.thefatrat.application.exceptions.BotErrorException;
import com.thefatrat.application.exceptions.BotException;
import com.thefatrat.application.sources.Direct;
import com.thefatrat.application.sources.Server;
import com.thefatrat.application.util.Colors;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
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
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.utils.Result;
import net.dv8tion.jda.api.utils.data.DataArray;
import net.dv8tion.jda.api.utils.data.DataObject;
import net.dv8tion.jda.internal.entities.ThreadMemberImpl;
import net.dv8tion.jda.internal.entities.channel.concrete.ThreadChannelImpl;
import net.dv8tion.jda.internal.requests.RestActionImpl;
import net.dv8tion.jda.internal.requests.Route;
import net.dv8tion.jda.internal.utils.Helpers;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    public RestAction<List<ThreadMember>> retrieveThreadMembers(ThreadChannel thread) {
        Route.CompiledRoute route = Route.Channels.LIST_THREAD_MEMBERS.compile(thread.getId());

        return new RestActionImpl<>(jda, route, (response, request) -> {
            List<RestAction<ThreadMember>> actions = new ArrayList<>();
            DataArray memberArr = response.getArray();

            for (int i = 0; i < memberArr.length(); ++i) {
                DataObject json = memberArr.getObject(i);

                actions.add(thread.getGuild().retrieveMemberById(json.getString("user_id"))
                    .map(member -> new ThreadMemberImpl(member, (ThreadChannelImpl) thread)
                        .setJoinedTimestamp(Helpers.toTimestamp(json.getString("join_timestamp")))
                        .setFlags(json.getInt("flags"))
                    )
                );
            }

            return RestAction.allOf(actions).complete();
        });
    }

    public RestAction<List<Guild>> retrieveMutualGuilds(UserSnowflake user) {
        List<RestAction<Result<Member>>> actions = jda.getGuilds().stream()
            .map(guild -> guild.retrieveMemberById(user.getId()).mapToResult())
            .toList();
        return RestAction.allOf(actions)
            .map(list -> list.stream()
                .filter(Result::isSuccess)
                .map(result -> result.get().getGuild())
                .toList()
            );
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        time = System.currentTimeMillis();
        jda.getPresence().setActivity(Activity.playing("DM me to contact mods"));
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
            event.deferReply().queue(hook -> {

                Reply reply = Reply.defaultInteractionReply(hook);

                try {
                    direct.clickButton(event.getUser().getId(), event.getComponentId(), event.getMessage(), reply);
                } catch (BotException e) {
                    reply.sendEmbedFormat(e.getColor(), e.getMessage());
                }
            });
        }
    }

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        if (event.getUser().isBot() || event.getUser().isSystem()) {
            return;
        }
        if (!event.isFromGuild()) {
            event.deferReply().queue(hook -> {

                Reply reply = Reply.defaultInteractionReply(hook);

                try {
                    direct.selectMenu(event.getUser().getId(), event.getComponentId(),
                        event.getInteraction().getValues().get(0), event.getMessage(), reply);
                } catch (BotException e) {
                    reply.sendEmbedFormat(e.getColor(), e.getMessage());
                }
            });
        }
    }

    @Override
    public void onMessageContextInteraction(@NotNull MessageContextInteractionEvent event) {
        if (event.getUser().isBot() || event.getUser().isSystem() || !event.isFromGuild()) {
            event.reply("Context interactions not allowed").queue();
            return;
        }
        Message message = event.getInteraction().getTarget();
        if (!message.getAuthor().isBot() ||
            !event.getJDA().getSelfUser().getId().equals(message.getAuthor().getId())) {
            event.deferReply(true).queue(hook ->
                hook.editOriginalEmbeds(new EmbedBuilder()
                    .setColor(Colors.RED)
                    .setDescription(BotErrorException.icon + " Message was not send by me")
                    .build()
                ).queue()
            );
            return;
        }
        Guild guild = Objects.requireNonNull(event.getGuild());
        event.deferReply(true).queue(hook -> {

            Reply reply = Reply.defaultInteractionReply(hook);

            try {
                servers.get(guild.getId())
                    .receiveInteraction(new InteractionEvent(message, event.getInteraction().getName()), reply);
            } catch (BotException e) {
                reply.sendEmbedFormat(e.getColor(), e.getMessage());
            }
        });
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (event.getUser().isBot() || event.getUser().isSystem() || !event.isFromGuild()) {
            event.reply("Slash commands not allowed").queue();
            return;
        }

        Guild guild = Objects.requireNonNull(event.getGuild());
        event.deferReply(false).queue(hook ->
            guild.retrieveMember(event.getUser()).queue(member -> {

                Map<String, OptionMapping> options = event.getOptions().stream()
                    .collect(Collectors.toMap(OptionMapping::getName, Function.identity()));

                Reply reply = new Reply() {

                    private final CountDownLatch latch = new CountDownLatch(1);
                    private BiConsumer<MessageEmbed, Consumer<Message>> sendEmbed = (e, c) -> {};
                    private boolean replied = false;

                    @Override
                    public void sendEmbed(MessageEmbed embed, Consumer<Message> callback) {
                        if (replied) {
                            try {
                                latch.await();
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                            sendEmbed.accept(embed, callback);
                            return;
                        }
                        replied = true;
                        hook.editOriginalEmbeds(embed).queue(callback.andThen(m -> {
                            sendEmbed = (e, c) -> m.getChannel().sendMessageEmbeds(e).queue(c);
                            latch.countDown();
                        }));
                    }
                };

                CommandEvent commandEvent = new CommandEvent(event.getName(), event.getSubcommandName(),
                    options, guild, event.getGuildChannel());

                try {
                    servers.get(guild.getId()).receiveCommand(commandEvent, reply);
                } catch (BotException e) {
                    reply.sendEmbedFormat(e.getColor(), e.getMessage());
                }
            })
        );
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

        servers.get(guild.getId()).getArchiveHandler().handle(archiveEvent);
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (event.getAuthor().isBot() || event.getAuthor().isSystem()) {
            return;
        }
        Message message = event.getMessage();

        Reply reply = Reply.defaultMessageReply(event.getMessage());

        try {
            if (message.isFromGuild()) {
                String id = message.getGuild().getId();
                servers.get(id).receiveMessage(message, reply);
            } else {
                direct.receiveMessage(message, reply);
            }
        } catch (BotException e) {
            reply.sendEmbedFormat(e.getColor(), e.getMessage());
        }
    }

}
