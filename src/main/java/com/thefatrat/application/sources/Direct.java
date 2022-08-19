package com.thefatrat.application.sources;

import com.thefatrat.application.Bot;
import com.thefatrat.application.entities.Reply;
import com.thefatrat.application.exceptions.BotErrorException;
import com.thefatrat.application.exceptions.BotException;
import com.thefatrat.application.exceptions.BotWarningException;
import com.thefatrat.application.handlers.MessageHandler;
import com.thefatrat.application.util.Colors;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Direct extends Source {

    private final Map<String, Message> handler = new HashMap<>();
    private final Map<String, SubmitRequest> submitter = new HashMap<>();

    @Override
    public void receiveMessage(Message message, Reply reply) {
        String author = message.getAuthor().getId();
        Thread thread = new Thread(() -> {

            List<Guild> mutual = Bot.getInstance().retrieveMutualGuilds(message.getAuthor()).complete();

            if (mutual.isEmpty()) {
                return;
            }
            if (mutual.size() == 1) {
                forward(mutual.get(0).getId(), message, reply);
                return;
            }

            if (handler.containsKey(author) || submitter.containsKey(author)) {
                throw new BotErrorException("Please submit or cancel your submission first");
            }

            EmbedBuilder embed = new EmbedBuilder()
                .setColor(Colors.BLUE)
                .setDescription("What server do you want to send this to?");

            List<Button> buttons = mutual.stream()
                .map(server ->
                    Button.primary(server.getId(), server.getName())
                )
                .collect(Collectors.toList());
            buttons.add(Button.danger("x", "Cancel"));

            message.getChannel()
                .sendMessageEmbeds(embed.build())
                .setActionRow(buttons)
                .queue();

            handler.put(message.getAuthor().getId(), message);
        });
        thread.setUncaughtExceptionHandler(new ChannelExceptionHandler(() -> {
            handler.remove(author);
            submitter.remove(author);
        }, reply));
        thread.start();
    }

    public void clickButton(String user, String button, Message message, Reply reply) {
        if (!handler.containsKey(user) && !submitter.containsKey(user)) {
            throw new BotErrorException("Something went wrong");
        }

        message.delete().queue();

        Thread thread = new Thread(() -> {
            if ("x".equals(button)) {
                handler.remove(user);
                submitter.remove(user);
                reply.sendEmbed(new EmbedBuilder()
                    .setColor(Colors.GREEN)
                    .setDescription(":white_check_mark: Successfully cancelled")
                    .build()
                );
                return;
            }

            if (!submitter.containsKey(user)) {
                forward(button, handler.remove(user), reply);
                return;
            }

            Server server = Bot.getInstance().getServer(submitter.get(user).server());
            MessageHandler messageHandler = server.getDirectHandler();
            if (!messageHandler.getKeys().contains(button)) {
                throw new BotErrorException("Could not send to the given service, try again");
            }
            messageHandler.handle(button, submitter.get(user).message(), reply);
            submitter.remove(user);
        });
        thread.setUncaughtExceptionHandler(new ChannelExceptionHandler(() -> {
            handler.remove(user);
            submitter.remove(user);
        }, reply));
        thread.start();
    }

    private void forward(String serverId, Message message, Reply reply) throws BotException {
        Server server = Bot.getInstance().getServer(serverId);
        if (server == null) {
            throw new BotErrorException("Something went wrong");
        }
        MessageHandler handler = server.getDirectHandler();
        if (handler.size() == 0) {
            throw new BotWarningException("The server does not handle any messages at the moment");
        }
        if (handler.size() == 1) {
            handler.handle(message, reply);
            return;
        }

        submitter.put(message.getAuthor().getId(), new SubmitRequest(serverId, message));

        List<Button> buttons = handler.getKeys().stream()
            .map(component ->
                Button.primary(component, component)
            )
            .collect(Collectors.toList());
        buttons.add(Button.danger("x", "Cancel"));

        reply.sendEmbed(new EmbedBuilder()
                .setColor(Colors.BLUE)
                .setDescription("What service do you want to send this to?")
                .build(),
            callback -> callback
                .editMessageComponents()
                .setActionRow(buttons)
                .queue()

        );

    }

    private record ChannelExceptionHandler(Runnable action, Reply reply)
        implements Thread.UncaughtExceptionHandler {

        @Override
        public void uncaughtException(Thread t, Throwable e) {
            if (e instanceof BotException error) {
                action.run();
                reply.sendEmbedFormat(error.getColor(), error.getMessage());
            }
        }

    }

    private record SubmitRequest(String server, Message message) {

    }

}
