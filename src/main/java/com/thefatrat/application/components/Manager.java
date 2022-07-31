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

    public Manager(Source server) {
        super(server, "default", true);
    }

    @Override
    public void register() {
        CommandHandler handler = getSource().getCommandHandler();

        handler.addListener("ping", command -> {
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
                channel.sendMessageFormat("Component `%s` enabled", component).queue();
            } else {
                channel.sendMessageFormat("Component `%s` does not exist", component).queue();
            }
        }, PermissionChecker.IS_ADMIN);

        handler.addListener("disable", command -> {
            if (command.args().length != 1 || !command.event().isFromGuild()) {
                return;
            }

            String component = command.args()[0];
            MessageChannel channel = command.event().getChannel();

            if (getSource().toggleComponent(component, false)) {
                channel.sendMessageFormat("Component `%s` disabled", component).queue();
            } else {
                channel.sendMessageFormat("Component `%s` does not exist", component).queue();
            }
        }, PermissionChecker.IS_ADMIN);

        handler.addListener("permission", command -> {
            if (command.args().length != 2 || !command.event().isFromGuild()) {
                return;
            }

            String componentString = command.args()[0];
            MessageChannel channel = command.event().getChannel();
            Component component = getSource().getComponent(componentString);

            if (component == null) {
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

}
