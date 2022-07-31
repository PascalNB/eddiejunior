package com.thefatrat.application.sources;

import com.thefatrat.application.Bot;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;

import java.util.List;

public class Direct extends Source {

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

    }

    private void forward(String id, Message message) {
        Bot.getInstance().getServer(id).getDirectHandler().handle(message);
    }

}
