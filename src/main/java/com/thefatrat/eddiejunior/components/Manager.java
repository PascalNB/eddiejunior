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
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.software.os.OSFileStore;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

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
                }),

            new Command("errorlog", "show the error log")
                .setAction(((command, reply) -> {
                    File log = Bot.getInstance().getLog();
                    if (log == null) {
                        throw new BotErrorException("Could not find log file");
                    }

                    try (BufferedReader reader = new BufferedReader(new FileReader(log))) {
                        String lines = reader.lines()
                            .map(line -> '`' + line + '`')
                            .collect(Collectors.joining(System.lineSeparator()));

                        if (lines.length() > 2048) {
                            lines = lines.substring(lines.length() - 2049);
                            if (lines.charAt(0) != '`') {
                                lines = '`' + lines;
                            }
                        }

                        reply.send(lines);
                    } catch (IOException e) {
                        throw new BotErrorException("Could not read log file");
                    }
                })),

            new Command("vitals", "show system vitals")
                .setAction((command, reply) -> {
                    reply.defer();

                    SystemInfo info = new SystemInfo();

                    CentralProcessor processor = info.getHardware().getProcessor();
                    GlobalMemory memory = info.getHardware().getMemory();
                    OSFileStore fileStore;
                    try {
                        fileStore = info.getOperatingSystem().getFileSystem().getFileStores().get(0);
                    } catch (IndexOutOfBoundsException e) {
                        throw new BotErrorException("No disk found");
                    }

                    long totalStorage = fileStore.getTotalSpace() / 1_048_576;
                    long totalMemory = memory.getTotal() / 1_048_576;

                    String vitals = String.format(Locale.ROOT, """
                            System: `%s`
                            OS: `%s`
                                                        
                            CPU: `%s`
                            - Cores: `%d`
                            - Usage: `%.2f%%`
                            - Speed: `%d MHz`
                            - Temp: `%.2f °C`
                                                        
                            Memory: `%d / %d MB`
                                                        
                            Storage: `%d / %d MB`
                            """,
                        info.getHardware().getComputerSystem().getModel(),
                        info.getOperatingSystem(),
                        processor.getProcessorIdentifier().getName().trim(),
                        processor.getLogicalProcessorCount(),
                        processor.getSystemCpuLoad(1000L) * 100.0,
                        processor.getProcessorIdentifier().getVendorFreq() / 1_000_000,
                        info.getHardware().getSensors().getCpuTemperature(),
                        totalMemory - memory.getAvailable() / 1_048_576,
                        totalMemory,
                        totalStorage - fileStore.getFreeSpace() / 1_048_576,
                        totalStorage
                    );

                    reply.send(new EmbedBuilder()
                        .setColor(Colors.TRANSPARENT)
                        .setTitle("System Vitals")
                        .setDescription(vitals)
                        .build());
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
