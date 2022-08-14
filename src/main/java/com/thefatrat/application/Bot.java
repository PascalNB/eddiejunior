package com.thefatrat.application;

import com.thefatrat.application.components.Component;
import com.thefatrat.application.exceptions.BotErrorException;
import com.thefatrat.application.exceptions.BotException;
import com.thefatrat.application.sources.Direct;
import com.thefatrat.application.sources.Server;
import com.thefatrat.application.sources.Source;
import com.thefatrat.application.util.Colors;
import com.thefatrat.application.util.CommandEvent;
import com.thefatrat.application.util.InteractionEvent;
import com.thefatrat.application.util.Reply;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Bot extends ListenerAdapter {

    private static Bot instance;

    private final Map<String, Source> sources = new HashMap<>();
    private Class<? extends Component>[] components;
    private long time = 0;
    private JDA jda = null;

    private Bot() {
        Source direct = new Direct();
        sources.put(null, direct);
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
        return (Server) sources.get(id);
    }

    @SafeVarargs
    public final void setComponents(Class<? extends Component>... components) {
        this.components = components;
    }

    private void loadServer(String id) {
        Server server = new Server(id);
        sources.put(server.getId(), server);
        server.registerComponents(components);
    }

    public String getUptime() {
        long t = System.currentTimeMillis() - time;
        long hours = TimeUnit.MILLISECONDS.toHours(t);
        long min = TimeUnit.MILLISECONDS.toMinutes(t);
        long sec = TimeUnit.MILLISECONDS.toSeconds(t);
        return String.format("%d hours, %d minutes, %d seconds",
            hours,
            min - TimeUnit.HOURS.toMinutes(hours),
            sec - TimeUnit.MINUTES.toSeconds(min)
        );
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
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

            Reply reply = new Reply() {
                @Override
                public void sendMessage(String message, Consumer<Message> callback) {
                    hook.editOriginal(message).queue(callback);
                }

                @Override
                public void sendEmbed(MessageEmbed embed, Consumer<Message> callback) {
                    hook.editOriginalEmbeds(embed).queue(callback);
                }
            };

            try {
                ((Server) sources.get(guild.getId()))
                    .receiveInteraction(new InteractionEvent(message,
                        event.getInteraction().getName()), reply);
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
        event.deferReply(false).queue(hook -> {
            Member member = guild.getMember(event.getUser());
            if (member == null) {
                member = guild.retrieveMember(event.getUser()).complete();
            }

            Map<String, OptionMapping> options = event.getOptions().stream()
                .collect(Collectors.toMap(OptionMapping::getName, Function.identity()));

            Reply reply = new Reply() {

                private final CountDownLatch latch = new CountDownLatch(1);
                private BiConsumer<String, Consumer<Message>> sendMessage = (s, c) -> {};
                private BiConsumer<MessageEmbed, Consumer<Message>> sendEmbed = (e, c) -> {};
                private boolean replied = false;

                @Override
                public void sendMessage(String message, Consumer<Message> callback) {
                    if (replied) {
                        try {
                            latch.await();
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        sendMessage.accept(message, callback);
                        return;
                    }
                    replied = true;
                    hook.editOriginal(message).queue(callback.andThen(m -> {
                        sendMessage = (s, c) -> m.getChannel().sendMessage(s).queue(c);
                        sendEmbed = (e, c) -> m.getChannel().sendMessageEmbeds(e).queue(c);
                        latch.countDown();
                    }));
                }

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
                        sendMessage = (s, c) -> m.getChannel().sendMessage(s).queue(c);
                        sendEmbed = (e, c) -> m.getChannel().sendMessageEmbeds(e).queue(c);
                        latch.countDown();
                    }));
                }
            };

            CommandEvent commandEvent = new CommandEvent(event.getName(), event.getSubcommandName(),
                options, guild, event.getChannel(), member);

            try {
                ((Server) sources.get(guild.getId())).receiveCommand(commandEvent, reply);
            } catch (BotException e) {
                reply.sendEmbedFormat(e.getColor(), e.getMessage());
            }
        });
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (event.getAuthor().isBot() || event.getAuthor().isSystem()) {
            return;
        }
        Message message = event.getMessage();

        Reply reply = new Reply() {
            @Override
            public void sendMessage(String message, Consumer<Message> callback) {
                event.getChannel().sendMessage(message).queue();
            }

            @Override
            public void sendEmbed(MessageEmbed embed, Consumer<Message> callback) {
                event.getChannel().sendMessageEmbeds(embed).queue();
            }
        };

        try {
            if (message.isFromGuild()) {
                String id = message.getGuild().getId();
                sources.get(id).receiveMessage(message, reply);
            } else {
                sources.get(null).receiveMessage(message, reply);
            }
        } catch (BotException e) {
            reply.sendEmbedFormat(e.getColor(), e.getMessage());
        }
    }

}
