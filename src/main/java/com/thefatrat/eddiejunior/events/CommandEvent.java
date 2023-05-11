package com.thefatrat.eddiejunior.events;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.unions.GuildMessageChannelUnion;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

import java.util.Map;

public class CommandEvent {

    private final String command;
    private final String subcommand;
    private final Map<String, OptionMapping> args;
    private final GuildMessageChannelUnion channel;
    private final Member member;

    public CommandEvent(String command, String subcommand, Map<String, OptionMapping> args,
        GuildMessageChannelUnion channel, Member member) {
        this.command = command;
        this.subcommand = subcommand;
        this.args = args;
        this.channel = channel;
        this.member = member;
    }

    public CommandEvent toSub() {
        return new CommandEvent(subcommand, null, args, channel, member);
    }

    public String getCommand() {
        return command;
    }

    public GuildMessageChannelUnion getChannel() {
        return channel;
    }

    public OptionMapping get(String option) {
        return args.get(option);
    }

    public boolean hasOption(String option) {
        return args.containsKey(option);
    }

    public Member getMember() {
        return member;
    }

}
