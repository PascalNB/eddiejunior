package com.thefatrat.application.components;

import com.thefatrat.application.entities.Command;
import com.thefatrat.application.exceptions.BotErrorException;
import com.thefatrat.application.sources.Server;
import com.thefatrat.application.util.URLUtil;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class Remove extends Component {

    public Remove(Server server) {
        super(server, "Remove", false);

        addCommands(new Command(getName(), "remove a message sent by me")
            .addOptions(new OptionData(OptionType.STRING, "message", "message jump url", true))
            .setAction((command, reply) -> {
                String url = command.getArgs().get("message").getAsString();
                Message message = URLUtil.messageFromURL(url, getServer().getGuild());

                if (!message.getAuthor().getId().equals(getServer().getGuild().getSelfMember().getId())) {
                    throw new BotErrorException("Message was not sent by me");
                }

                message.delete().queue(success -> reply.hide().ok("Removed message"));
            })
        );
    }

}
