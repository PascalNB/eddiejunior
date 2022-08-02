package com.thefatrat.application.components;

import com.thefatrat.application.Bot;
import com.thefatrat.application.PermissionChecker;
import com.thefatrat.application.sources.Server;
import com.thefatrat.application.util.Command;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class Manager extends Component {

    public static final String NAME = "Default";

    public Manager(Server server) {
        super(server, NAME, true);

        addCommands(
            new Command("ping", "check the RTT of the connection in milliseconds")
                .setAction((command, reply) -> {
                    final long start = System.currentTimeMillis();
                    String content = "pong :ping_pong:";
                    reply.sendMessage(content, message -> {
                        long time = System.currentTimeMillis() - start;
                        message.editOriginalFormat("%s %d ms", content, time).queue();
                    });
                }),

            new Command("enable", "enable a specific component by name")
                .addOption(new OptionData(OptionType.STRING, "component", "component name", true))
                .setAction((command, reply) -> {
                    String component = command.getArgs().get("component").getAsString();

                    if (getServer().toggleComponent(component, true)) {
                        reply.sendMessageFormat(
                            ":ballot_box_with_check: Component `%s` enabled",
                            component
                        );
                    } else {
                        componentNotFound(component);
                    }
                })
                .setPermissions(PermissionChecker.IS_ADMIN),

            new Command("disable", "disable a specific component by name")
                .addOption(new OptionData(OptionType.STRING, "component", "component name", true))
                .setAction((command, reply) -> {
                    String component = command.getArgs().get("component").getAsString();

                    Component component1 = getServer().getComponent(component);
                    if (component1 instanceof DirectComponent direct) {
                        if (direct.isRunning()) {
                            direct.stop(command, reply);
                        }
                    }

                    if (getServer().toggleComponent(component, false)) {
                        reply.sendMessageFormat(":no_entry: Component `%s` disabled", component);
                    } else {
                        componentNotFound(component);
                    }
                })
                .setPermissions(PermissionChecker.IS_ADMIN),

            new Command("components", "shows a list of all the components")
                .setAction((command, reply) -> {
                    StringBuilder builder = new StringBuilder()
                        .append("All components:");
                    for (Component component : getServer().getComponents()) {
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
                    reply.sendMessage(builder.toString());
                })
                .setPermissions(PermissionChecker.IS_ADMIN),

            new Command("permission", "allow the given roles to manage the given component")
                .addOption(new OptionData(OptionType.STRING, "component", "component name", true))
                .addOption(new OptionData(OptionType.ROLE, "role", "role", true))
                .setAction((command, reply) -> {
                    String componentString = command.getArgs().get("component").getAsString();
                    Component component = getServer().getComponent(componentString);

                    if (component == null || component == this) {
                        componentNotFound(componentString);
                        return;
                    }

                    Role role = command.getArgs().get("role").getAsRole();

                    getServer().getCommandHandler().getCommand(componentString).addRoles(role);

                    reply.sendMessageFormat(":unlock: Command permissions for `%s (%s)` for " +
                            "component `%s` have been granted",
                        role.getName(), role.getId(), componentString
                    );
                })
                .setPermissions(PermissionChecker.IS_ADMIN),

            new Command("revoke", "revoke all roles from managing the given component")
                .addOption(new OptionData(OptionType.STRING, "component", "component name", true))
                .addOption(new OptionData(OptionType.ROLE, "role", "role", false))
                .setAction((command, reply) -> {

                    String componentString = command.getArgs().get("component").getAsString();
                    Component component = getServer().getComponent(componentString);

                    if (component == null || component == this) {
                        componentNotFound(componentString);
                    }

                    if (!command.getArgs().containsKey("role")) {
                        getServer().getCommandHandler().getCommand(
                            componentString).removeAllRoles();
                        reply.sendMessageFormat(
                            ":lock: Command permissions for component `%s` have been revoked",
                            componentString
                        );
                        return;
                    }

                    Role role = command.getArgs().get("role").getAsRole();

                    getServer().getCommandHandler().getCommand(componentString).removeRoles(role);

                    reply.sendMessageFormat(":lock: Command permissions for `%s (%s)` for " +
                            "component `%s` have been revoked",
                        role.getName(), role.getId(), componentString
                    );
                })
                .setPermissions(PermissionChecker.IS_ADMIN),

            new Command("status", "shows the current status of the bot")
                .addOption(new OptionData(OptionType.STRING, "component", "component name", false))
                .setAction((command, reply) -> {
                    Component component;

                    if (command.getArgs().containsKey("component")) {
                        String componentString = command.getArgs().get("component").getAsString();
                        component = getServer().getComponent(componentString);

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

                    reply.sendEmbed(embed);
                })
                .setPermissions(PermissionChecker.IS_ADMIN)
        );
    }

    @Override
    public int getColor() {
        return 0xba3d52;
    }

    @Override
    public String getStatus() {
        long count = getServer().getComponents().stream().filter(Component::isEnabled).count() - 1;
        return String.format("""
                Components enabled: %d
                Uptime: %s
                """,
            count,
            Bot.getInstance().getUptime());
    }

}
