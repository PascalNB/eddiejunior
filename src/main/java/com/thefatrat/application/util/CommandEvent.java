package com.thefatrat.application.util;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

import java.util.Map;

public class CommandEvent {

    private final String command;
    private final String subcommand;
    private final Map<String, OptionMapping> args;
    private final Guild guild;
    private final MessageChannel channel;
    private final Member member;

    public CommandEvent(String command, String subcommand, Map<String, OptionMapping> args,
        Guild guild, MessageChannel channel, Member member) {
        this.command = command;
        this.subcommand = subcommand;
        this.args = args;
        this.guild = guild;
        this.channel = channel;
        this.member = member;
    }

    public CommandEvent toSub() {
        return new CommandEvent(subcommand, null, args, guild, channel, member);
    }

    public String getCommand() {
        return command;
    }

    public String getSubcommand() {
        return subcommand;
    }

    public Guild getGuild() {
        return guild;
    }

    public Member getMember() {
        return member;
    }

    public MessageChannel getChannel() {
        return channel;
    }

    public Map<String, OptionMapping> getArgs() {
        return args;
    }

}
