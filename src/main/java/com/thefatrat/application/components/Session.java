package com.thefatrat.application.components;

import com.thefatrat.application.entities.Command;
import com.thefatrat.application.exceptions.BotErrorException;
import com.thefatrat.application.exceptions.BotWarningException;
import com.thefatrat.application.reply.Reply;
import com.thefatrat.application.sources.Server;
import com.thefatrat.application.util.Colors;
import com.thefatrat.application.util.Icon;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.IMentionable;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.PermissionOverride;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.internal.utils.PermissionUtil;

import java.util.*;

public class Session extends Component {

    public static final String NAME = "Session";

    public static final String ERROR_SESSION_NONEXISTENT = String.format("The given session does not exist, " +
        "use `/%s list` for the list of available sessions", NAME.toLowerCase());
    private static final Permission[] PERMISSIONS = new Permission[]{
        Permission.VIEW_CHANNEL,
        Permission.MANAGE_PERMISSIONS};

    private final Map<String, Set<String>> sessions = new HashMap<>();

    public Session(Server server) {
        super(server, NAME, false);

        {
            List<String> list = getDatabaseManager().getSettings("session");
            for (String session : list) {
                sessions.put(session, new HashSet<>());
                List<String> ids = getDatabaseManager().getSettings("session_" + session);
                for (String id : ids) {
                    sessions.get(session).add(id);
                }
            }
        }

        setComponentCommand();

        addSubcommands(
            new Command("list", "list all sessions")
                .setAction((command, reply) -> {
                    if (sessions.isEmpty()) {
                        throw new BotWarningException("There are no sessions added yet");
                    }

                    List<String> list = new ArrayList<>(sessions.size());
                    for (String session : sessions.keySet()) {
                        list.add('`' + session + '`');
                    }
                    String joined = String.join("\n", list.toArray(new String[0]));

                    reply.send(new EmbedBuilder()
                        .setTitle("Sessions")
                        .setColor(Colors.TRANSPARENT)
                        .setDescription(joined)
                        .build()
                    );
                }),
            new Command("add",
                "add a channel to a session, creates a new session if it does not exist")
                .addOption(new OptionData(OptionType.STRING, "session", "session name", true)
                    .setRequiredLength(3, 20)
                )
                .addOption(new OptionData(OptionType.CHANNEL, "channel", "channel", true))
                .setAction((command, reply) -> {
                    String session = command.getArgs().get("session").getAsString();
                    if (!sessions.containsKey(session)) {
                        sessions.put(session, new HashSet<>());
                        getDatabaseManager().addSetting("session", session);
                    }
                    IMentionable channel = command.getArgs().get("channel").getAsChannel();
                    sessions.get(session).add(channel.getId());
                    getDatabaseManager().addSetting("session_" + session, channel.getId());

                    reply.ok("%s added to session `%s`", channel.getAsMention(), session);
                }),
            new Command("remove",
                "removes a channel from a session, removes the session if no channels are left")
                .addOptions(
                    new OptionData(OptionType.STRING, "session", "session name", true)
                        .setRequiredLength(3, 20),
                    new OptionData(OptionType.CHANNEL, "channel", "channel", false)
                )
                .setAction((command, reply) -> {
                    String string = command.getArgs().get("session").getAsString();
                    if (!isSession(string)) {
                        throw new BotErrorException(ERROR_SESSION_NONEXISTENT);
                    }
                    if (command.getArgs().containsKey("channel")) {
                        IMentionable channel = command.getArgs().get("channel").getAsChannel();
                        Set<String> session = sessions.get(string);
                        if (!session.contains(channel.getId())) {
                            throw new BotErrorException("The given channel is a not part of the session");
                        }

                        session.remove(channel.getId());
                        getDatabaseManager().removeSetting("session_" + string, channel.getId());
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
                .addOption(new OptionData(OptionType.STRING, "session", "session name", true)
                    .setRequiredLength(3, 20)
                )
                .setAction((command, reply) -> {
                    String string = command.getArgs().get("session").getAsString();
                    if (!isSession(string)) {
                        throw new BotErrorException(ERROR_SESSION_NONEXISTENT);
                    }

                    Set<String> session = sessions.get(string);
                    Guild guild = getServer().getGuild();
                    List<String> channels = new ArrayList<>();
                    for (String id : session) {
                        IMentionable channel = guild.getGuildChannelById(id);
                        if (channel == null) {
                            sessions.get(string).remove(id);
                        } else {
                            channels.add(channel.getAsMention());
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
                .addOption(new OptionData(OptionType.STRING, "session", "session name", true)
                    .setRequiredLength(3, 20)
                )
                .setAction((command, reply) -> {
                    String session = command.getArgs().get("session").getAsString();
                    if (!isSession(session)) {
                        throw new BotErrorException(ERROR_SESSION_NONEXISTENT);
                    }
                    openSession(session, reply);
                }),
            new Command("close", "closes channels of a session")
                .addOption(new OptionData(OptionType.STRING, "session", "session name", true)
                    .setRequiredLength(3, 20)
                )
                .setAction((command, reply) -> {
                    String session = command.getArgs().get("session").getAsString();
                    if (!isSession(session)) {
                        throw new BotErrorException("The given session does not exist");
                    }
                    closeSession(session, reply);
                })
        );

    }

    public void openSession(String session, Reply reply) {
        Set<String> sessionChannels = sessions.get(session);
        Guild guild = getServer().getGuild();
        List<RestAction<PermissionOverride>> actions = new ArrayList<>();
        for (String id : sessionChannels) {
            GuildChannel channel = guild.getGuildChannelById(id);
            if (channel == null) {
                sessions.get(session).remove(id);
            } else {
                checkPermissions(channel);
                actions.add(channel.getPermissionContainer()
                    .upsertPermissionOverride(getServer().getGuild().getPublicRole())
                    .grant(Permission.VIEW_CHANNEL));
            }
        }

        String[] array = executeOnChannels(session, actions);
        String joined = String.join(", ", array);

        reply.ok("Session `%s` started.%n" +
            "The following channels have been made public:%n%s", session, joined);
    }

    public void closeSession(String session, Reply reply) {
        Set<String> sessionChannels = sessions.get(session);
        Guild guild = getServer().getGuild();
        List<RestAction<PermissionOverride>> actions = new ArrayList<>();
        for (String id : sessionChannels) {
            GuildChannel channel = guild.getGuildChannelById(id);
            if (channel == null) {
                sessions.get(session).remove(id);
            } else {
                checkPermissions(channel);
                actions.add(channel.getPermissionContainer()
                    .upsertPermissionOverride(getServer().getGuild().getPublicRole())
                    .deny(Permission.VIEW_CHANNEL));
            }
        }

        String[] array = executeOnChannels(session, actions);
        String joined = String.join(", ", array);

        reply.send(Icon.STOP, "Session `%s` stopped.%n" +
            "The following channels have been made private:%n%s", session, joined);
    }

    public boolean isSession(String session) {
        return sessions.containsKey(session);
    }

    private String[] executeOnChannels(String session, List<RestAction<PermissionOverride>> actions) {
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
        Member member = getServer().getGuild().getSelfMember();
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
