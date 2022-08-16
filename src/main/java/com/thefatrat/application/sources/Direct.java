package com.thefatrat.application.sources;

import com.thefatrat.application.Bot;
import com.thefatrat.application.entities.Reply;
import com.thefatrat.application.exceptions.BotErrorException;
import com.thefatrat.application.exceptions.BotException;
import com.thefatrat.application.exceptions.BotWarningException;
import com.thefatrat.application.handlers.DirectHandler;
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

    private final DirectHandler handler = new DirectHandler();
    private final Map<String, SubmitRequest> submitter = new HashMap<>();

    @Override
    public void receiveMessage(Message message, Reply reply) {
        Thread thread = new Thread(() -> {
            String author = message.getAuthor().getId();

            List<Guild> mutual = message.getAuthor().getMutualGuilds();
            if (mutual.isEmpty()) {
                for (Guild server : Bot.getInstance().getJDA().getGuilds()) {
                    if (!mutual.contains(server)
                        && !server
                        .findMembers(member -> member.getId().equals(author))
                        .get()
                        .isEmpty()) {
                        mutual.add(server);
                    }
                }
            }

            if (mutual.isEmpty()) {
                return;
            }
            if (mutual.size() == 1) {
                forward(mutual.get(0).getId(), message, reply);
                return;
            }

            if (handler.contains(author) || submitter.containsKey(author)) {
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

            handler.addUser(message.getAuthor(), message);
        });
        thread.setUncaughtExceptionHandler(new ChannelExceptionHandler(reply));
        thread.start();
    }

    public void clickButton(String user, String button, Message message, Reply reply) {
        if (!handler.contains(user) && !submitter.containsKey(user)) {
            throw new BotErrorException("Something went wrong");
        }

        message.delete().queue();

        Thread thread = new Thread(() -> {
            if ("x".equals(button)) {
                handler.removeUser(user);
                submitter.remove(user);
                reply.sendEmbed(new EmbedBuilder()
                    .setColor(Colors.GREEN)
                    .setDescription(":white_check_mark: Successfully cancelled")
                    .build()
                );
                return;
            }

            if (!submitter.containsKey(user)) {
                Message m = handler.getMessage(user);
                handler.removeUser(user);
                forward(button, m, reply);
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
        thread.setUncaughtExceptionHandler(new ChannelExceptionHandler(reply));
        thread.start();
    }

    private void forward(String id, Message message, Reply reply) throws BotException {
        Server server = Bot.getInstance().getServer(id);
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

        submitter.put(message.getAuthor().getId(), new SubmitRequest(id, message));

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

    private record ChannelExceptionHandler(Reply reply)
        implements Thread.UncaughtExceptionHandler {

        @Override
        public void uncaughtException(Thread t, Throwable e) {
            if (e instanceof BotException error) {
                reply.sendEmbedFormat(error.getColor(), error.getMessage());
            }
        }

    }

    private record SubmitRequest(String server, Message message) {

    }

}
