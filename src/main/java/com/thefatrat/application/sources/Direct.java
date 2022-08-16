package com.thefatrat.application.sources;

import com.thefatrat.application.Bot;
import com.thefatrat.application.exceptions.BotException;
import com.thefatrat.application.exceptions.BotWarningException;
import com.thefatrat.application.handlers.DirectHandler;
import com.thefatrat.application.handlers.MessageHandler;
import com.thefatrat.application.util.Colors;
import com.thefatrat.application.entities.Reply;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;

import java.util.List;

public class Direct extends Source {

    private final DirectHandler handler = new DirectHandler();

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

            if (!handler.contains(author)) {
                StringBuilder builder = new StringBuilder()
                    .append("What server do you want to send this to?").append("\n");

                for (int i = 0; i < mutual.size(); i++) {
                    builder.append(String.format("`%d: %s`%n", i, mutual.get(i).getName()));
                }

                builder.append("Reply with the number or name of the server, or `x` to cancel");
                message.getChannel().sendMessage(builder.toString()).queue();
                handler.addUser(message.getAuthor(), mutual, message);
                return;
            }

            List<Guild> storedMutual = handler.getMutual(author);
            String content = message.getContentRaw().trim();

            if ("x".equalsIgnoreCase(content)) {
                handler.removeUser(author);
                message.getChannel()
                    .sendMessageEmbeds(new EmbedBuilder()
                        .setColor(Colors.GREEN)
                        .setDescription(":white_check_mark: Successfully cancelled")
                        .build()
                    )
                    .queue();
                return;
            }

            try {
                int i = Integer.parseInt(content);
                if (i >= 0 && i < storedMutual.size()) {
                    Message m = handler.getMessage(author);
                    handler.removeUser(author);
                    forward(storedMutual.get(i).getId(), m, reply);
                    return;
                }
            } catch (NumberFormatException e) {
                for (Guild server : storedMutual) {
                    if (server.getName().trim().equalsIgnoreCase(content)) {
                        Message m = handler.getMessage(author);
                        handler.removeUser(author);
                        forward(server.getId(), m, reply);
                        return;
                    }
                }
            }

            throw new BotWarningException("Please select a valid server");
        });
        thread.setUncaughtExceptionHandler(new ChannelExceptionHandler(reply));
        thread.start();
    }

    private void forward(String id, Message message, Reply reply) throws BotException {
        MessageHandler handler = Bot.getInstance().getServer(id).getDirectHandler();
        if (handler.size() == 0) {
            throw new BotWarningException("The server does not handle any messages at the moment");
        }
        handler.handle(message, reply);
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

}
