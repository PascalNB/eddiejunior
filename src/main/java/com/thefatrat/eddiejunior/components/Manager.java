package com.thefatrat.eddiejunior.components;

import com.pascalnb.dbwrapper.Database;
import com.pascalnb.dbwrapper.DatabaseException;
import com.thefatrat.eddiejunior.Bot;
import com.thefatrat.eddiejunior.entities.Command;
import com.thefatrat.eddiejunior.exceptions.BotErrorException;
import com.thefatrat.eddiejunior.exceptions.BotException;
import com.thefatrat.eddiejunior.exceptions.BotWarningException;
import com.thefatrat.eddiejunior.sources.Server;
import com.thefatrat.eddiejunior.util.Colors;
import com.thefatrat.eddiejunior.util.Icon;
import com.thefatrat.eddiejunior.util.PermissionChecker;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.IMentionable;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class Manager extends Component {

    public static final String NAME = "Main";

    public Manager(Server server) {
        super(server, NAME, true);

        String logChannelId = getDatabaseManager().getSetting("log");
        if (logChannelId != null) {
            getServer().setLog(getServer().getGuild().getTextChannelById(logChannelId));
        }

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
                    component.enable();
                    getServer().toggleComponent(component, true).queue();
                    reply.send(Icon.ENABLE, "Component `%s` enabled", componentString);
                    getServer().log(Colors.BLUE, command.getMember().getUser(),
                        "Enabled component `%s`", componentString);
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
                    component.disable();
                    getServer().toggleComponent(component, false).queue();
                    reply.send(Icon.DISABLE, "Component `%s` disabled", componentString);
                    getServer().log(Colors.BLUE, command.getMember().getUser(),
                        "Disabled component `%s`", componentString);
                }),

            new Command("components", "shows a list of all the components")
                .setAction((command, reply) -> {
                    EmbedBuilder embed = new EmbedBuilder()
                        .setColor(Colors.TRANSPARENT)
                        .setTitle("Components");

                    int empty = 0;
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
                            }
                            if (component instanceof DirectComponent direct && direct.isRunning()) {
                                builder.append("Listens to DMs\n");
                            }
                        }

                        embed.addField(component.getTitle(), builder.toString(), true);
                        empty++;
                    }

                    empty = (3 - empty % 3) % 3;
                    for (int i = 0; i < empty; i++) {
                        embed.addBlankField(true);
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
                }),

            new Command("reload", "reload a component's commands")
                .addOption(new OptionData(OptionType.STRING, "component", "component name", true))
                .setAction((command, reply) -> {
                    Component component = getComponentSafe(command.getArgs().get("component").getAsString());
                    getServer().toggleComponent(component, false).queue(__ ->
                        getServer().toggleComponent(component, true).queue());
                    reply.ok("Reloaded component `%s`", component.getName());
                }),

            new Command("log", "set the logging channel")
                .addOption(new OptionData(OptionType.CHANNEL, "channel", "log channel", true)
                    .setChannelTypes(ChannelType.TEXT)
                )
                .setAction((command, reply) -> {
                    TextChannel channel = command.getArgs().get("channel").getAsChannel().asTextChannel();
                    PermissionChecker.requireSend(channel);
                    getServer().setLog(channel);
                    getDatabaseManager().setSetting("log", channel.getId());
                    reply.ok("Set logging channel to %s", channel.getAsMention());
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
                Log: %s
                """,
            count,
            Bot.getInstance().getUptime(),
            Optional.ofNullable(getServer().getLog()).map(IMentionable::getAsMention).orElse(null));
    }

}
