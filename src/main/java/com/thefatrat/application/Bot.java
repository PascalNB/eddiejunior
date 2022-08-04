package com.thefatrat.application;

import com.thefatrat.application.components.Component;
import com.thefatrat.application.exceptions.BotException;
import com.thefatrat.application.sources.Direct;
import com.thefatrat.application.sources.Server;
import com.thefatrat.application.sources.Source;
import com.thefatrat.application.util.CommandEvent;
import com.thefatrat.application.util.Reply;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
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
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (event.getUser().isBot() || event.getUser().isSystem()) {
            event.reply("Only slash commands by users allowed").queue();
            return;
        }
        if (!event.isFromGuild()) {
            event.reply("Slash commands only allowed in servers").queue();
            return;
        }

        Objects.requireNonNull(event.getGuild());
        event.deferReply(false).queue(hook -> {
            Member member = event.getGuild().getMember(event.getUser());
            if (member == null) {
                member = event.getGuild().retrieveMember(event.getUser()).complete();
            }
            String id = event.getGuild().getId();

            Map<String, OptionMapping> options = event.getOptions().stream()
                .collect(Collectors.toMap(OptionMapping::getName, Function.identity()));

            Reply reply = new Reply() {

                private final CountDownLatch latch = new CountDownLatch(1);
                private BiConsumer<String, Consumer<Message>> sendMessage = (s, c) -> {};
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
                    hook.editOriginal(message).queue(callback.andThen(m -> {
                        sendMessage = (s, c) -> m.getChannel().sendMessage(s).queue(c);
                        latch.countDown();
                    }));
                    replied = true;
                }

                @Override
                public void sendEmbed(MessageEmbed embed) {
                    replied = true;
                    hook.editOriginalEmbeds(embed).queue();
                }
            };

            CommandEvent commandEvent = new CommandEvent(event.getName(), event.getSubcommandName(),
                options, event.getGuild(), event.getChannel(), member);

            try {
                ((Server) sources.get(id)).receiveCommand(commandEvent, reply);
            } catch (BotException e) {
                reply.sendMessage(e.getMessage());
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
            public void sendEmbed(MessageEmbed embed) {
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
            reply.sendMessage(e.getMessage());
        }
    }

}
