package com.thefatrat.application.events;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.unions.GuildMessageChannelUnion;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

import java.util.Map;

public class CommandEvent {

    private final String command;
    private final String subcommand;
    private final Map<String, OptionMapping> args;
    private final Guild guild;
    private final GuildMessageChannelUnion channel;
    private final User user;

    public CommandEvent(String command, String subcommand, Map<String, OptionMapping> args,
        Guild guild, GuildMessageChannelUnion channel, User user) {
        this.command = command;
        this.subcommand = subcommand;
        this.args = args;
        this.guild = guild;
        this.channel = channel;
        this.user = user;
    }

    public CommandEvent toSub() {
        return new CommandEvent(subcommand, null, args, guild, channel, user);
    }

    public String getCommand() {
        return command;
    }

    public Guild getGuild() {
        return guild;
    }

    public GuildMessageChannelUnion getChannel() {
        return channel;
    }

    public Map<String, OptionMapping> getArgs() {
        return args;
    }

    public User getUser() {
        return user;
    }

}
