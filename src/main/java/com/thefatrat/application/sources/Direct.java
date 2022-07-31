package com.thefatrat.application.sources;

import com.thefatrat.application.Bot;
import com.thefatrat.application.handlers.DirectHandler;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;

import java.util.List;

public class Direct extends Source {

    private final DirectHandler handler = new DirectHandler();

    @Override
    public void receiveMessage(Message message) {
        List<Guild> mutual = message.getAuthor().getMutualGuilds();
        if (mutual.size() == 0) {
            return;
        }
        if (mutual.size() == 1) {
            forward(mutual.get(0).getId(), message);
            return;
        }

        String author = message.getAuthor().getId();

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
                forward(storedMutual.get(i).getId(), handler.getMessage(author));
                handler.removeUser(author);
                return;
            }
        } catch (NumberFormatException e) {
            for (Guild server : storedMutual) {
                if (server.getName().trim().equalsIgnoreCase(content)) {
                    forward(server.getId(), handler.getMessage(author));
                    handler.removeUser(author);
                    return;
                }
            }
        }

        message.getChannel().sendMessage(":warning: Please select a valid server").queue();
    }

    private void forward(String id, Message message) {
        Bot.getInstance().getServer(id).getDirectHandler().handle(message);
    }

}
