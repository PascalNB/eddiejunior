package com.thefatrat.application.sources;

import com.thefatrat.application.Bot;
import com.thefatrat.application.exceptions.BotException;
import com.thefatrat.application.exceptions.BotWarningException;
import com.thefatrat.application.handlers.DirectHandler;
import com.thefatrat.application.handlers.MessageHandler;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;

import java.util.List;

public class Direct extends Source {

    private final DirectHandler handler = new DirectHandler();

    @Override
    public void receiveMessage(Message message) {
        Thread thread = new Thread(() -> {
            String author = message.getAuthor().getId();

            List<Guild> mutual = message.getAuthor().getMutualGuilds();
            if (mutual.size() == 0) {
                for (Guild server : message.getJDA().getGuilds()) {
                    if (!mutual.contains(server)
                        && server
                        .findMembers(member -> member.getId().equals(author))
                        .get()
                        .size() != 0) {
                        mutual.add(server);
                    }
                }
            }

            if (mutual.size() == 0) {
                return;
            }
            if (mutual.size() == 1) {
                forward(mutual.get(0).getId(), message);
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
                    .sendMessage(":white_check_mark: Successfully cancelled")
                    .queue();
                return;
            }

            try {
                int i = Integer.parseInt(content);
                if (i >= 0 && i < storedMutual.size()) {
                    Message m = handler.getMessage(author);
                    handler.removeUser(author);
                    forward(storedMutual.get(i).getId(), m);
                    return;
                }
            } catch (NumberFormatException e) {
                for (Guild server : storedMutual) {
                    if (server.getName().trim().equalsIgnoreCase(content)) {
                        Message m = handler.getMessage(author);
                        handler.removeUser(author);
                        forward(server.getId(), m);
                        return;
                    }
                }
            }

            throw new BotWarningException("Please select a valid server");
        });
        thread.setUncaughtExceptionHandler(new ChannelExceptionHandler(message.getChannel()));
        thread.start();
    }

    private void forward(String id, Message message) throws BotException {
        MessageHandler handler = Bot.getInstance().getServer(id).getDirectHandler();
        if (handler.size() == 0) {
            throw new BotWarningException("The server does not handle any messages at the moment");
        }
        handler.handle(message);
    }

    private record ChannelExceptionHandler(MessageChannel channel)
        implements Thread.UncaughtExceptionHandler {

        @Override
        public void uncaughtException(Thread t, Throwable e) {
            channel.sendMessage(e.getMessage()).queue();
        }

    }

}
