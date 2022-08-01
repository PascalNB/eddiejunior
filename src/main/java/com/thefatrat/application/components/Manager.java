package com.thefatrat.application.components;

import com.thefatrat.application.Bot;
import com.thefatrat.application.Command;
import com.thefatrat.application.HelpEmbedBuilder;
import com.thefatrat.application.PermissionChecker;
import com.thefatrat.application.exceptions.BotErrorException;
import com.thefatrat.application.exceptions.BotWarningException;
import com.thefatrat.application.handlers.CommandHandler;
import com.thefatrat.application.sources.Server;
import com.thefatrat.application.sources.Source;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;

import java.util.ArrayList;
import java.util.List;

// TODO: overall permissions
public class Manager extends Component {

    public static final String NAME = "Default";

    private final MessageEmbed help;

    public Manager(Source server) {
        super(server, NAME, true);
        help = new HelpEmbedBuilder(NAME)
            .addCommand("help", "shows this message")
            .addCommand("help [component]", "show the help message for the given component")
            .addCommand("ping", "check the RTT of the connection in milliseconds")
            .addCommand("status", "shows the current status of the bot")
            .addCommand("status [component]", "shows the status of the given component")
            .addCommand("prefix", "set the prefix for all commands, - by default")
            .addCommand("enable [component]", "enable a specific component by name")
            .addCommand("disable [component]", "disable a specific component by name")
            .addCommand("permission [component] [roles]",
                "allow the given roles to manage the given component")
            .addCommand("revoke [component]", "revoke all roles from managing the given component")
            .addCommand("revoke [component] [roles]",
                "revoke component permissions of the given roles")
            .build(getColor());
    }

    @Override
    public int getColor() {
        return 0xba3d52;
    }

    @Override
    public MessageEmbed getHelp() {
        return help;
    }

    @Override
    public void register() {
        CommandHandler handler = getSource().getCommandHandler();

        handler.addListener("help", command -> {
            if (!command.message().isFromGuild()) {
                return;
            }

            if (command.args().length == 0) {
                command.message().getChannel().sendMessageEmbeds(getHelp()).queue();
                return;
            }

            String componentString = command.args()[0];
            Component component = getSource().getComponent(componentString);
            if (component == null) {
                componentNotFound(componentString);
                return;
            }

            command.message().getChannel().sendMessageEmbeds(component.getHelp()).queue();

        });

        handler.addListener("ping", command -> {
            if (!command.message().isFromGuild()) {
                return;
            }
            final long start = System.currentTimeMillis();
            command.message().getChannel()
                .sendMessage("pong :ping_pong:")
                .queue(message -> {
                    long time = System.currentTimeMillis() - start;
                    message.editMessageFormat("%s %d ms", message.getContentRaw(), time).queue();
                });
        });

        handler.addListener("prefix", command -> {
            if (command.args().length != 1 || !command.message().isFromGuild()) {
                return;
            }
            String prefix = command.args()[0];

            if (prefix.length() > 3) {
                throw new BotWarningException("Prefix should not be longer than 3 characters");
            }

            Bot.getInstance().getServer(command.message().getGuild().getId())
                .setPrefix(prefix);
            command.message().getChannel().sendMessageFormat(
                ":white_check_mark: Prefix set to `%s`", prefix).queue();

        }, PermissionChecker.IS_ADMIN);

        handler.addListener("enable", command -> {
            if (command.args().length != 1 || !command.message().isFromGuild()) {
                return;
            }

            String component = command.args()[0];
            MessageChannel channel = command.message().getChannel();

            if (getSource().toggleComponent(component, true)) {
                channel.sendMessageFormat(
                    ":ballot_box_with_check: Component `%s` enabled",
                    component
                ).queue();
            } else {
                componentNotFound(component);
            }
        }, PermissionChecker.IS_ADMIN);

        handler.addListener("disable", command -> {
            if (command.args().length != 1 || !command.message().isFromGuild()) {
                return;
            }

            String component = command.args()[0];
            MessageChannel channel = command.message().getChannel();

            Component component1 = getSource().getComponent(component);
            if (component1 instanceof DirectComponent direct) {
                if (direct.isRunning()) {
                    direct.stop(command);
                }
            }

            if (getSource().toggleComponent(component, false)) {
                channel.sendMessageFormat(":no_entry: Component `%s` disabled", component).queue();
            } else {
                componentNotFound(component);
            }
        }, PermissionChecker.IS_ADMIN);

        handler.addListener("components", command -> {
            if (!command.message().isFromGuild()) {
                return;
            }

            StringBuilder builder = new StringBuilder()
                .append("All components:");
            for (Component component : getSource().getComponents()) {
                builder.append("\n- ").append(component.getTitle());

                if (component.isEnabled()) {
                    if (component.isAlwaysEnabled()) {
                        builder.append(" :lock:");
                    } else {
                        builder.append(" :ballot_box_with_check:");
                    }

                    if (component instanceof DirectComponent direct) {
                        if (direct.isPaused()) {
                            builder.append(" :pause_button:");
                        } else if (direct.isRunning()) {
                            builder.append(" :white_check_mark:");
                        }
                    }
                }
            }

            command.message().getChannel().sendMessage(builder.toString()).queue();

        });

        handler.addListener("permission", command -> {
            if (command.args().length < 2 || !command.message().isFromGuild()) {
                return;
            }

            String componentString = command.args()[0];
            MessageChannel channel = command.message().getChannel();
            Component component = getSource().getComponent(componentString);

            if (component == null || component == this) {
                componentNotFound(componentString);
                return;
            }

            Role[] roles = parseRoles(command, 1);

            if (roles.length == 0) {
                String message = command.args().length < 3
                    ? "The given role was not found"
                    : "One or more roles were not found";
                throw new BotErrorException(message);
            }

            getSource().getCommandHandler().getCommand(componentString).addRoles(roles);

            StringBuilder builder = new StringBuilder();
            for (Role role : roles) {
                builder.append(role).append(", ");
            }
            builder.delete(builder.length() - 2, builder.length());

            channel.sendMessageFormat(
                ":unlock: Command permissions for `%s` for component `%s` have been granted",
                builder.toString(), componentString
            ).queue();

        }, PermissionChecker.IS_ADMIN);

        handler.addListener("revoke", command -> {
            if (command.args().length == 0 || !command.message().isFromGuild()) {
                return;
            }

            String componentString = command.args()[0];
            MessageChannel channel = command.message().getChannel();
            Component component = getSource().getComponent(componentString);

            if (component == null || component == this) {
                componentNotFound(componentString);
            }

            if (command.args().length == 1) {
                getSource().getCommandHandler().getCommand(componentString).removeAllRoles();
                channel.sendMessageFormat(
                    ":lock: Command permissions for component `%s` have been revoked",
                    componentString
                ).queue();
                return;
            }

            Role[] roles = parseRoles(command, 1);

            if (roles.length == 0) {
                String message = command.args().length < 3
                    ? "The given role was not found"
                    : "One or more roles were not found";
                throw new BotErrorException(message);
            }

            getSource().getCommandHandler().getCommand(componentString).removeRoles(roles);

            StringBuilder builder = new StringBuilder();
            for (Role role : roles) {
                builder.append(role).append(", ");
            }
            builder.delete(builder.length() - 2, builder.length());

            channel.sendMessageFormat(
                ":lock: Command permissions for `%s` for component `%s` have been revoked",
                builder.toString(), componentString
            ).queue();

        }, PermissionChecker.IS_ADMIN);

        handler.addListener("status", command -> {
            if (!command.message().isFromGuild()) {
                return;
            }

            Component component;

            if (command.args().length > 0) {
                String componentString = command.args()[0];
                component = getSource().getComponent(componentString);

                if (component == null) {
                    componentNotFound(componentString);
                    return;
                }

            } else {
                component = this;
            }

            MessageEmbed embed = new EmbedBuilder()
                .setTitle(component.getTitle())
                .setColor(component.getColor())
                .setDescription(component.getStatus())
                .build();

            command.message().getChannel().sendMessageEmbeds(embed).queue();

        }, PermissionChecker.IS_ADMIN);
    }

    @Override
    public String getStatus() {
        long count = getSource().getComponents().stream().filter(Component::isEnabled).count() - 1;
        return String.format("""
                Prefix: `%s`
                Components enabled: %d
                Uptime: %s
                """,
            ((Server) getSource()).getPrefix(),
            count,
            Bot.getInstance().getUptime());
    }

    public static Role[] parseRoles(Command command, int position) {
        if (command.args().length == 0) {
            return new Role[0];
        }
        List<Role> result = new ArrayList<>(command.message().getMentions().getRoles());

        if (result.size() != 0) {
            return result.toArray(Role[]::new);
        }

        for (int i = position; i < command.args().length; i++) {
            long id;
            try {
                id = Long.parseLong(command.args()[i]);
            } catch (NumberFormatException e) {
                continue;
            }
            Role role = command.message().getGuild().getRoleById(id);
            if (role != null) {
                result.add(role);
            }
        }

        if (result.size() != 0) {
            return result.toArray(Role[]::new);
        }

        return new Role[0];
    }

}
