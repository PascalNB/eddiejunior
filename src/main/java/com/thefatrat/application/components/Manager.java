package com.thefatrat.application.components;

import com.thefatrat.application.Bot;
import com.thefatrat.application.PermissionChecker;
import com.thefatrat.application.handlers.CommandHandler;
import com.thefatrat.application.sources.Source;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.Role;

import java.util.Objects;

// TODO: overall permissions
public class Manager extends Component {

    public static final String NAME = "Manager";

    public Manager(Source server) {
        super(server, NAME, true);
    }

    @Override
    public void register() {
        CommandHandler handler = getSource().getCommandHandler();

        // TODO specific help for components
        handler.addListener("help", command -> {
            if (!command.event().isFromGuild()) {
                return;
            }
            StringBuilder builder = new StringBuilder();
            for (Component component : getSource().getComponents()) {
                if (component.isEnabled()) {
                    builder.append("**").append(component.getName().toUpperCase())
                        .append("**\n").append(component.getHelp());
                }
            }
            command.event().getChannel().sendMessage(builder.toString()).queue();
        });

        handler.addListener("ping", command -> {
            if (!command.event().isFromGuild()) {
                return;
            }
            final long start = System.currentTimeMillis();
            command.event().getChannel()
                .sendMessage("pong :ping_pong:")
                .queue(message -> {
                    long time = System.currentTimeMillis() - start;
                    message.editMessageFormat("%s %d ms", message.getContentRaw(), time).queue();
                });
        });

        handler.addListener("setprefix", command -> {
            if (command.args().length != 1 || !command.event().isFromGuild()) {
                return;
            }
            Bot.getInstance().getServer(command.event().getGuild().getId())
                .setPrefix(command.args()[0]);
        }, PermissionChecker.IS_ADMIN);

        handler.addListener("enable", command -> {
            if (command.args().length != 1 || !command.event().isFromGuild()) {
                return;
            }

            String component = command.args()[0];
            MessageChannel channel = command.event().getChannel();

            if (getSource().toggleComponent(component, true)) {
                channel.sendMessageFormat(
                    ":ballot_box_with_check: Component `%s` enabled",
                    component
                ).queue();
            } else {
                channel.sendMessageFormat(":x: Component `%s` does not exist", component).queue();
            }
        }, PermissionChecker.IS_ADMIN);

        handler.addListener("disable", command -> {
            if (command.args().length != 1 || !command.event().isFromGuild()) {
                return;
            }

            String component = command.args()[0];
            MessageChannel channel = command.event().getChannel();

            Component component1 = getSource().getComponent(component);
            if (component1 instanceof DirectComponent direct) {
                if (direct.isRunning()) {
                    direct.stop(command);
                }
            }

            if (getSource().toggleComponent(component, false)) {
                channel.sendMessageFormat(":no_entry: Component `%s` disabled", component).queue();
            } else {
                channel.sendMessageFormat(":x: Component `%s` does not exist", component).queue();
            }
        }, PermissionChecker.IS_ADMIN);

        handler.addListener("components", command -> {
            if (!command.event().isFromGuild()) {
                return;
            }

            StringBuilder builder = new StringBuilder()
                .append("All components:");
            for (Component component : getSource().getComponents()) {
                builder.append("\n- ").append(component.getName());

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

            command.event().getChannel().sendMessage(builder.toString()).queue();

        });

        handler.addListener("permission", command -> {
            if (command.args().length != 2 || !command.event().isFromGuild()) {
                return;
            }

            String componentString = command.args()[0];
            MessageChannel channel = command.event().getChannel();
            Component component = getSource().getComponent(componentString);

            if (component == null || component == this) {
                channel.sendMessageFormat("Component `%s` does not exist", componentString).queue();
                return;
            }

            Role role;

            try {
                long id = Long.parseLong(command.args()[1]);
                role = Objects.requireNonNull(command.event().getJDA().getRoleById(id));
            } catch (NumberFormatException | NullPointerException e) {
                channel.sendMessageFormat("The given role ID was not found",
                    componentString).queue();
                return;
            }

            getSource().getCommandHandler().setPredicate(componentString,
                PermissionChecker.hasAnyRole(role).or(PermissionChecker.IS_ADMIN));

            channel.sendMessageFormat("Role `%s (%s)` now has command permissions for component " +
                    "`%s`", role.getName(), role.getId(), componentString)
                .queue();

        }, PermissionChecker.IS_ADMIN);

        handler.addListener("revoke", command -> {
            if (command.args().length != 1 || !command.event().isFromGuild()) {
                return;
            }

            String componentString = command.args()[0];
            MessageChannel channel = command.event().getChannel();
            Component component = getSource().getComponent(componentString);

            if (component == null) {
                channel.sendMessageFormat("Component `%s` does not exist", componentString).queue();
                return;
            }

            getSource().getCommandHandler().setPredicate(componentString,
                PermissionChecker.IS_ADMIN);

            channel.sendMessageFormat("Command permissions for component `%s` have been reset",
                    componentString)
                .queue();

        }, PermissionChecker.IS_ADMIN);
    }

    @Override
    public String getHelp() {
        return """
            `help`
              - shows this message.
            `ping`
              - check the RTT of the connection in milliseconds.
            `setprefix`
              - set the prefix for all commands, - by default.
            `enable [component]`
              - enable a specific component by name.
            `disable [component]`
              - disable a specific component by name.
            `permission [component] [role id]`
              - allow the given role to manage the given component.
            `revoke [component]`
              - revoke all roles from managing the given component.
            """;
    }

}
