package com.thefatrat.eddiejunior.components.impl;

import com.thefatrat.eddiejunior.components.AbstractComponent;
import com.thefatrat.eddiejunior.entities.Command;
import com.thefatrat.eddiejunior.entities.UserRole;
import com.thefatrat.eddiejunior.exceptions.BotErrorException;
import com.thefatrat.eddiejunior.exceptions.BotWarningException;
import com.thefatrat.eddiejunior.reply.Reply;
import com.thefatrat.eddiejunior.sources.Server;
import com.thefatrat.eddiejunior.util.Colors;
import com.thefatrat.eddiejunior.util.Icon;
import com.thefatrat.eddiejunior.util.URLUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.internal.utils.PermissionUtil;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class SessionComponent extends AbstractComponent {

    public static final String NAME = "Session";

    public static final String ERROR_SESSION_NONEXISTENT = String.format("The given session does not exist, " +
        "use `/%s list` for the list of available sessions", NAME.toLowerCase());
    private static final Permission[] PERMISSIONS = new Permission[]{
        Permission.VIEW_CHANNEL,
        Permission.MANAGE_PERMISSIONS};

    private final Map<String, Map<String, Message>> sessions = new HashMap<>();

    public SessionComponent(Server server) {
        super(server, NAME);

        {
            List<String> list = getDatabaseManager().getSettings("session");
            for (String session : list) {
                Map<String, Message> map = new HashMap<>();
                sessions.put(session, map);
                List<String> ids = getDatabaseManager().getSettings("session_" + session);
                for (String id : ids) {
                    String messageString = getDatabaseManager().getSetting("message_" + session + "_" + id);
                    Message message = URLUtil.getMessageFromString(messageString, getGuild());
                    map.put(id, message);
                }
            }
        }

        setComponentCommand(UserRole.MANAGE);

        addSubcommands(
            new Command("list", "list all sessions")
                .setRequiredUserRole(UserRole.USE)
                .setAction((command, reply) -> {
                    if (sessions.isEmpty()) {
                        throw new BotWarningException("There are no sessions added yet");
                    }

                    EmbedBuilder embedBuilder = new EmbedBuilder()
                        .setTitle("Sessions")
                        .setColor(Colors.TRANSPARENT);

                    sessions.entrySet().stream()
                        .limit(MessageEmbed.MAX_FIELD_AMOUNT)
                        .forEach(entry -> {
                            StringBuilder stringBuilder = new StringBuilder();
                            entry.getValue().forEach((channelId, message) -> {
                                stringBuilder.append("<#").append(channelId).append(">\n");
                                if (message != null) {
                                    stringBuilder.append("- ").append(message.getJumpUrl()).append("\n");
                                }
                            });
                            if (!stringBuilder.isEmpty()) {
                                stringBuilder.deleteCharAt(stringBuilder.length() - 1);
                                embedBuilder.addField(entry.getKey(), stringBuilder.toString(), false);
                            }
                        });

                    reply.send(embedBuilder.build());
                }),

            new Command("add", "add a channel to a session, creates a new session if it does not exist")
                .addOptions(
                    new OptionData(OptionType.STRING, "session", "session name", true)
                        .setRequiredLength(3, 20),
                    new OptionData(OptionType.CHANNEL, "channel", "channel", true)
                )
                .setAction((command, reply) -> {
                    String session = command.get("session").getAsString();
                    if (!sessions.containsKey(session)) {
                        sessions.put(session, new HashMap<>());
                        getDatabaseManager().addSetting("session", session);
                    }
                    IMentionable channel = command.get("channel").getAsChannel();
                    sessions.get(session).put(channel.getId(), null);
                    getDatabaseManager().addSetting("session_" + session, channel.getId());

                    reply.ok("%s added to session `%s`", channel.getAsMention(), session);
                }),

            new Command("setmessage", "set the message to be sent in the given channel during the given session")
                .addOptions(
                    new OptionData(OptionType.STRING, "session", "session name", true)
                        .setRequiredLength(3, 20),
                    new OptionData(OptionType.CHANNEL, "channel", "channel", true)
                        .setChannelTypes(ChannelType.TEXT),
                    new OptionData(OptionType.STRING, "message", "message url", true)
                )
                .setAction((command, reply) -> {
                    String session = command.get("session").getAsString();
                    if (!isSession(session)) {
                        throw new BotErrorException(ERROR_SESSION_NONEXISTENT);
                    }
                    TextChannel channel = command.get("channel").getAsChannel().asTextChannel();
                    String channelId = channel.getId();
                    Map<String, Message> map = sessions.get(session);
                    if (!map.containsKey(channelId)) {
                        throw new BotErrorException("Channel %s is not a part of session `%s`",
                            channel.getAsMention(), session);
                    }

                    String url = command.get("message").getAsString();
                    Message message = URLUtil.messageFromURL(url, getGuild());
                    String messageString = message.getChannel().getId() + "_" + message.getId();
                    map.put(channelId, message);
                    getDatabaseManager().setSetting("message_" + session + "_" + channelId, messageString);
                    reply.ok("Set message for session `%s`", session);
                }),

            new Command("removemessage", "remove the message from the session")
                .addOptions(
                    new OptionData(OptionType.STRING, "session", "session name", true),
                    new OptionData(OptionType.CHANNEL, "channel", "channel", true)
                        .setChannelTypes(ChannelType.TEXT)
                )
                .setAction((command, reply) -> {
                    String session = command.get("session").getAsString();
                    if (!isSession(session)) {
                        throw new BotErrorException(ERROR_SESSION_NONEXISTENT);
                    }
                    TextChannel channel = command.get("channel").getAsChannel().asTextChannel();

                    Map<String, Message> map = sessions.get(session);
                    if (!map.containsKey(channel.getId())) {
                        throw new BotErrorException("Channel %s is not a part of session `%s`",
                            channel.getAsMention(), session);
                    }
                    if (map.get(channel.getId()) == null) {
                        throw new BotErrorException("Channel %s does not have an associated message for session `%s`",
                            channel.getAsMention(), session);
                    }

                    map.put(channel.getId(), null);
                    getDatabaseManager().removeSetting("message_" + session + "_" + channel.getId());
                    reply.ok("Removed message from channel %s for session `%s`", channel.getAsMention(), session);
                }),

            new Command("remove", "removes a channel from a session, removes the session if no channels are left")
                .addOptions(
                    new OptionData(OptionType.STRING, "session", "session name", true)
                        .setRequiredLength(3, 20),
                    new OptionData(OptionType.CHANNEL, "channel", "channel", false)
                )
                .setAction((command, reply) -> {
                    String string = command.get("session").getAsString();
                    if (!isSession(string)) {
                        throw new BotErrorException(ERROR_SESSION_NONEXISTENT);
                    }
                    if (command.hasOption("channel")) {
                        IMentionable channel = command.get("channel").getAsChannel();
                        Map<String, Message> session = sessions.get(string);
                        if (!session.containsKey(channel.getId())) {
                            throw new BotErrorException("The given channel is a not part of the session");
                        }

                        session.remove(channel.getId());
                        getDatabaseManager().removeSetting("session_" + string, channel.getId());
                        getDatabaseManager().removeSetting("message_" + string + "_" + channel.getId());
                        if (session.isEmpty()) {
                            sessions.remove(string);
                            removeSessionFromDatabase(string);
                        }
                        reply.ok("%s removed from session `%s`", channel.getAsMention(), string);
                    } else {
                        removeSessionFromDatabase(string);
                        sessions.remove(string);
                        reply.ok("Session `%s` has been removed", string);
                    }
                }),

            new Command("show", "shows the channels of the given session")
                .setRequiredUserRole(UserRole.USE)
                .addOptions(new OptionData(OptionType.STRING, "session", "session name", true)
                    .setRequiredLength(3, 20)
                )
                .setAction((command, reply) -> {
                    String string = command.get("session").getAsString();
                    if (!isSession(string)) {
                        throw new BotErrorException(ERROR_SESSION_NONEXISTENT);
                    }

                    Map<String, Message> session = sessions.get(string);
                    Guild guild = getGuild();
                    List<String> channels = new ArrayList<>();
                    for (Map.Entry<String, Message> entry : session.entrySet()) {
                        IMentionable channel = guild.getGuildChannelById(entry.getKey());
                        if (channel != null) {
                            Message message = entry.getValue();
                            String listString = channel.getAsMention();
                            if (message != null) {
                                listString += " Message: <" + message.getJumpUrl() + ">";
                            }
                            channels.add(listString);
                        } else {
                            session.remove(entry.getKey());
                            getDatabaseManager().removeSetting("message_" + string + "_" + entry.getKey());
                        }
                    }

                    if (channels.isEmpty()) {
                        sessions.remove(string);
                        removeSessionFromDatabase(string);
                        throw new BotWarningException(
                            "The given session did not have existing channels and has been removed");
                    }

                    String[] array = channels.toArray(String[]::new);
                    String joined = String.join("\n", array);

                    reply.send(new EmbedBuilder()
                        .setColor(Colors.TRANSPARENT)
                        .setTitle("Session " + string)
                        .setDescription(joined)
                        .build()
                    );
                }),

            new Command("open", "opens channels of a session")
                .setRequiredUserRole(UserRole.USE)
                .addOptions(new OptionData(OptionType.STRING, "session", "session name", true)
                    .setRequiredLength(3, 20)
                )
                .setAction((command, reply) -> {
                    String session = command.get("session").getAsString();
                    if (!isSession(session)) {
                        throw new BotErrorException(ERROR_SESSION_NONEXISTENT);
                    }
                    getServer().log(Colors.GREEN, command.getMember().getUser(), "Opened session `%s`", session);
                    openSession(session, reply);
                }),

            new Command("close", "closes channels of a session")
                .setRequiredUserRole(UserRole.USE)
                .addOptions(new OptionData(OptionType.STRING, "session", "session name", true)
                    .setRequiredLength(3, 20)
                )
                .setAction((command, reply) -> {
                    String session = command.get("session").getAsString();
                    if (!isSession(session)) {
                        throw new BotErrorException("The given session does not exist");
                    }
                    getServer().log(Colors.RED, command.getMember().getUser(), "Closed session `%s`", session);
                    closeSession(session, reply);
                })
        );
    }

    public void openSession(String session, Reply reply) {
        Map<String, Message> sessionChannels = sessions.get(session);
        Guild guild = getGuild();
        List<RestAction<PermissionOverride>> actions = new ArrayList<>();
        for (String id : sessionChannels.keySet()) {
            GuildChannel channel = guild.getGuildChannelById(id);
            if (channel == null) {
                sessionChannels.remove(id);
            } else {
                checkPermissions(channel);
                actions.add(channel.getPermissionContainer()
                    .upsertPermissionOverride(getGuild().getPublicRole())
                    .grant(Permission.VIEW_CHANNEL));
            }
        }

        String[] array = executeOnChannels(session, actions);
        String joined = String.join(", ", array);

        reply.ok("Session `%s` started.%n" +
            "The following channels have been made public:%n%s", session, joined);

        for (Map.Entry<String, Message> entry : sessionChannels.entrySet()) {
            TextChannel channel = guild.getTextChannelById(entry.getKey());
            if (channel != null && channel.canTalk()) {
                Message message = entry.getValue();
                if (message != null) {
                    channel.sendMessage(MessageCreateData.fromMessage(message)).queue();
                }
            }
        }

        getServer().log("Opened %s", joined);
    }

    public void closeSession(String session, Reply reply) {
        Set<String> sessionChannels = sessions.get(session).keySet();
        Guild guild = getGuild();
        List<RestAction<PermissionOverride>> actions = new ArrayList<>();
        for (String id : sessionChannels) {
            GuildChannel channel = guild.getGuildChannelById(id);
            if (channel == null) {
                sessions.get(session).remove(id);
            } else {
                checkPermissions(channel);
                actions.add(channel.getPermissionContainer()
                    .upsertPermissionOverride(getGuild().getPublicRole())
                    .deny(Permission.VIEW_CHANNEL));
            }
        }

        String[] array = executeOnChannels(session, actions);
        String joined = String.join(", ", array);

        reply.send(Icon.STOP, "Session `%s` stopped.%n" +
            "The following channels have been made private:%n%s", session, joined);
        getServer().log("Closed %s", joined);
    }

    public boolean isSession(String session) {
        return sessions.containsKey(session);
    }

    private String[] executeOnChannels(String session, @NotNull List<RestAction<PermissionOverride>> actions) {
        if (actions.isEmpty()) {
            sessions.remove(session);
            removeSessionFromDatabase(session);
            throw new BotWarningException(
                "The given session did not have existing channels and has been removed");
        }

        List<PermissionOverride> overrides = RestAction.allOf(actions).complete();

        List<String> channels = new ArrayList<>();
        for (PermissionOverride override : overrides) {
            channels.add(override.getChannel().getAsMention());
        }

        return channels.toArray(String[]::new);
    }

    private void checkPermissions(GuildChannel channel) {
        Member member = getGuild().getSelfMember();
        for (Permission permission : PERMISSIONS) {
            if (!PermissionUtil.checkPermission(channel.getPermissionContainer(), member, permission)) {
                throw new BotErrorException("Insufficient permissions, requires `%s` in %s",
                    permission.getName(), channel.getAsMention());
            }
        }
    }

    private void removeSessionFromDatabase(String session) {
        getDatabaseManager().removeSetting("session_" + session);
        getDatabaseManager().removeSetting("session", session);
        for (String id : sessions.get(session).keySet()) {
            getDatabaseManager().removeSetting("message_" + session + "_" + id);
        }
    }

    @Override
    public String getStatus() {
        return String.format("""
                Enabled: %b
                Sessions: %d
                """,
            isEnabled(), sessions.size());
    }

}
