package com.thefatrat.application.components;

import com.thefatrat.application.Bot;
import com.thefatrat.application.sources.Server;
import com.thefatrat.application.util.Colors;
import com.thefatrat.application.util.Command;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class Manager extends Component {

    public static final String NAME = "Default";

    public Manager(Server server) {
        super(server, NAME, true);

        addCommands(
            new Command("help", "show the available commands")
                .addOption(new OptionData(OptionType.STRING, "component", "component name", false))
                .setAction((command, reply) -> {
                    if (command.getArgs().containsKey("component")) {
                        String componentString = command.getArgs().get("component").getAsString();
                        Component component = getServer().getComponent(componentString);

                        if (component == null) {
                            componentNotFound(componentString);
                            return;
                        }

                        reply.sendEmbed(component.getHelp());
                    } else {
                        reply.sendEmbed(getHelp());
                    }
                }),

            new Command("ping", "check the RTT of the connection in milliseconds")
                .setAction((command, reply) -> {
                    final long start = System.currentTimeMillis();
                    reply.sendEmbedFormat(message -> {
                            long time = System.currentTimeMillis() - start;
                            message.editMessageEmbeds(new EmbedBuilder()
                                .setColor(getColor())
                                .setDescription(String.format(":ping_pong: %d ms", time))
                                .build()
                            ).queue();
                        }, getColor(), "..."
                    );
                }),

            new Command("enable", "enable a specific component by name")
                .addOption(new OptionData(OptionType.STRING, "component", "component name", true))
                .setAction((command, reply) -> {
                    String componentString = command.getArgs().get("component").getAsString();
                    Component component = getServer().getComponent(componentString);

                    if (component == null) {
                        componentNotFound(componentString);
                        return;
                    }
                    component.getDatabaseManager().toggleComponent(true);
                    getServer().toggleComponent(component, true);

                    reply.sendEmbedFormat(Colors.BLUE,
                        ":ballot_box_with_check: Component `%s` enabled",
                        componentString);
                }),

            new Command("disable", "disable a specific component by name")
                .addOption(new OptionData(OptionType.STRING, "component", "component name", true))
                .setAction((command, reply) -> {
                    String componentString = command.getArgs().get("component").getAsString();

                    Component component = getServer().getComponent(componentString);
                    if (component == null) {
                        componentNotFound(componentString);
                        return;
                    }

                    if (component instanceof DirectComponent direct) {
                        if (direct.isRunning()) {
                            direct.stop(command, reply);
                        }
                    }

                    component.getDatabaseManager().toggleComponent(false);
                    getServer().toggleComponent(component, false);

                    reply.sendEmbedFormat(Colors.BLUE, ":no_entry: Component `%s` disabled",
                        componentString);
                }),

            new Command("components", "shows a list of all the components")
                .setAction((command, reply) -> {
                    StringBuilder builder = new StringBuilder();
                    for (Component component : getServer().getComponents()) {
                        builder.append(component.getTitle());

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
                        builder.append("\n");
                    }
                    builder.deleteCharAt(builder.length() - 1);
                    reply.sendEmbed(new EmbedBuilder()
                        .setColor(Colors.WHITE)
                        .addField("Components", builder.toString(), false)
                        .build()
                    );
                }),

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
