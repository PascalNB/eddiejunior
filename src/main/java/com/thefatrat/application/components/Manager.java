package com.thefatrat.application.components;

import com.thefatrat.application.Bot;
import com.thefatrat.application.entities.Command;
import com.thefatrat.application.exceptions.BotErrorException;
import com.thefatrat.application.exceptions.BotException;
import com.thefatrat.application.exceptions.BotWarningException;
import com.thefatrat.application.sources.Server;
import com.thefatrat.application.util.Colors;
import com.thefatrat.application.util.Icon;
import com.thefatrat.application.util.PermissionChecker;
import com.thefatrat.database.Database;
import com.thefatrat.database.DatabaseException;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.NotNull;

public class Manager extends Component {

    public static final String NAME = "Main";

    public Manager(Server server) {
        super(server, NAME, true);

        addCommands(
            new Command("help", "show the available commands")
                .addOption(new OptionData(OptionType.STRING, "component", "component name", false))
                .setAction((command, reply) -> {
                    if (command.getArgs().containsKey("component")) {
                        String componentString = command.getArgs().get("component").getAsString();
                        Component component = getComponentSafe(componentString);

                        reply.send(component.getHelp());
                    } else {
                        reply.send(getHelp());
                    }
                }),

            new Command("ping", "check the RTT of the connection in milliseconds")
                .setAction((command, reply) -> {
                    PermissionChecker.requireSend(command.getChannel().getPermissionContainer());

                    reply.send(new EmbedBuilder()
                            .setColor(Colors.TRANSPARENT)
                            .addField("WebSocket", Bot.getInstance().getJDA().getGatewayPing() + " ms", true)
                            .build(),
                        message -> {
                            MessageEmbed embed = message.getEmbeds().get(0);
                            try {
                                long start2 = System.currentTimeMillis();
                                Database database = Database.getInstance().connect();
                                long time2 = System.currentTimeMillis() - start2;
                                message.editMessageEmbeds(new EmbedBuilder(embed)
                                    .addField("Database", time2 + " ms", true)
                                    .build()
                                ).queue();
                                database.close();
                            } catch (DatabaseException e) {
                                message.editMessageEmbeds(new EmbedBuilder(embed)
                                    .addField("Database", ":x:", true)
                                    .build()
                                ).queue();
                            }
                        });
                }),

            new Command("enable", "enable a specific component by name")
                .addOption(new OptionData(OptionType.STRING, "component", "component name", true))
                .setAction((command, reply) -> {
                    String componentString = command.getArgs().get("component").getAsString();
                    Component component = getComponentSafe(componentString);

                    if (component.isGlobalComponent()) {
                        throw new BotWarningException("This component is always enabled");
                    }

                    component.getDatabaseManager().toggleComponent(true);
                    getServer().toggleComponent(component, true).queue();
                    reply.send(Icon.ENABLE, "Component `%s` enabled", componentString);
                }),

            new Command("disable", "disable a specific component by name")
                .addOption(new OptionData(OptionType.STRING, "component", "component name", true))
                .setAction((command, reply) -> {
                    String componentString = command.getArgs().get("component").getAsString();

                    Component component = getComponentSafe(componentString);

                    if (component.isGlobalComponent()) {
                        throw new BotWarningException("This component is always enabled");
                    }

                    if (component instanceof DirectComponent direct && direct.isRunning()) {
                        direct.stop(reply);
                    }

                    component.getDatabaseManager().toggleComponent(false);
                    getServer().toggleComponent(component, false).queue();
                    reply.send(Icon.DISABLE, "Component `%s` disabled", componentString);
                }),

            new Command("components", "shows a list of all the components")
                .setAction((command, reply) -> {
                    EmbedBuilder embed = new EmbedBuilder()
                        .setColor(Colors.TRANSPARENT)
                        .setTitle("Components");

                    for (Component component : getServer().getComponents()) {
                        if (component.isGlobalComponent()) {
                            continue;
                        }
                        StringBuilder builder = new StringBuilder();
                        builder.append("Enabled: ").append(component.isEnabled() ? Icon.ENABLE : Icon.DISABLE)
                            .append("\n");

                        if (component.isEnabled()) {
                            if (component instanceof RunnableComponent runComp) {
                                builder.append("Running: ").append(runComp.isRunning() ? Icon.OK : Icon.ERROR)
                                    .append("\n");
                                builder.append("Runs auto: ").append(runComp.isAutoRunnable() ? Icon.OK : Icon.ERROR)
                                    .append("\n");
                            }
                            if (component instanceof DirectComponent direct && direct.isRunning()) {
                                builder.append("Listens to DMs\n");
                            }
                        }

                        embed.addField(component.getTitle(), builder.toString(), true);
                    }
                    reply.send(embed.build());
                }),

            new Command("status", "shows the current status of the bot")
                .addOption(new OptionData(OptionType.STRING, "component", "component name", false))
                .setAction((command, reply) -> {
                    Component component;

                    if (command.getArgs().containsKey("component")) {
                        String componentString = command.getArgs().get("component").getAsString();
                        component = getComponentSafe(componentString);

                    } else {
                        component = this;
                    }

                    MessageEmbed embed = new EmbedBuilder()
                        .setColor(Colors.TRANSPARENT)
                        .setTitle(component.getTitle() + " status")
                        .setDescription(component.getStatus())
                        .build();

                    reply.send(embed);
                })
        );
    }

    /**
     * Throws specific {@link BotErrorException} with the given component name.
     *
     * @param component the component name
     * @throws BotException always
     */
    @NotNull
    private Component getComponentSafe(String component) throws BotException {
        Component c = getServer().getComponent(component);
        if (c != null) {
            return c;
        }
        throw new BotErrorException(String.format("Component `%s` does not exist", component));
    }

    @Override
    public String getStatus() {
        int count = 0;
        for (Component component : getServer().getComponents()) {
            if (component.isEnabled() && !component.isGlobalComponent()) {
                ++count;
            }
        }
        return String.format("""
                Components enabled: %d
                Uptime: %s
                """,
            count,
            Bot.getInstance().getUptime());
    }

}
