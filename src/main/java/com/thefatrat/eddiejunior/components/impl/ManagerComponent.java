package com.thefatrat.eddiejunior.components.impl;

import com.pascalnb.dbwrapper.Database;
import com.pascalnb.dbwrapper.DatabaseException;
import com.thefatrat.eddiejunior.Bot;
import com.thefatrat.eddiejunior.DatabaseManager;
import com.thefatrat.eddiejunior.builders.HelpMessageBuilder;
import com.thefatrat.eddiejunior.components.AbstractComponent;
import com.thefatrat.eddiejunior.components.Component;
import com.thefatrat.eddiejunior.components.GlobalComponent;
import com.thefatrat.eddiejunior.components.RunnableComponent;
import com.thefatrat.eddiejunior.entities.Command;
import com.thefatrat.eddiejunior.entities.UserRole;
import com.thefatrat.eddiejunior.events.CommandEvent;
import com.thefatrat.eddiejunior.exceptions.BotErrorException;
import com.thefatrat.eddiejunior.exceptions.BotException;
import com.thefatrat.eddiejunior.exceptions.BotWarningException;
import com.thefatrat.eddiejunior.reply.InteractionReply;
import com.thefatrat.eddiejunior.sources.Server;
import com.thefatrat.eddiejunior.util.Colors;
import com.thefatrat.eddiejunior.util.Icon;
import com.thefatrat.eddiejunior.util.PermissionChecker;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.IMentionable;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.jetbrains.annotations.NotNull;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ManagerComponent extends AbstractComponent implements GlobalComponent {

    private final Map<String, MessageEmbed> helpMessages = new HashMap<>();

    public ManagerComponent(Server server) {
        super(server, "main");

        String logChannelId = getDatabaseManager().getSetting("log");
        if (logChannelId != null) {
            getServer().setLog(getGuild().getTextChannelById(logChannelId));
        }
        String manageRoleId = getDatabaseManager().getSetting("managerole");
        if (manageRoleId != null) {
            Role manageRole = getGuild().getRoleById(manageRoleId);
            getServer().setManageRole(manageRole);
        }
        String useRoleId = getDatabaseManager().getSetting("userole");
        if (useRoleId != null) {
            Role useRole = getGuild().getRoleById(useRoleId);
            getServer().setUseRole(useRole);
        }

        addCommands(
            new Command("help", "show the available commands")
                .setRequiredUserRole(UserRole.USE)
                .addOptions(new OptionData(OptionType.STRING, "component", "component name", false))
                .setAction(this::help),

            new Command("ping", "check the RTT of the connection in milliseconds")
                .setRequiredUserRole(UserRole.USE)
                .setAction(this::ping),

            new Command("enable", "enable a specific component by name")
                .setRequiredUserRole(UserRole.MANAGE)
                .addOptions(new OptionData(OptionType.STRING, "component", "component name", true))
                .setAction(this::enableComponent),

            new Command("disable", "disable a specific component by name")
                .setRequiredUserRole(UserRole.MANAGE)
                .addOptions(new OptionData(OptionType.STRING, "component", "component name", true))
                .setAction(this::disableComponent),

            new Command("components", "shows a list of all the components")
                .setRequiredUserRole(UserRole.USE)
                .setAction(this::listComponents),

            new Command("status", "shows the current status of the bot")
                .setRequiredUserRole(UserRole.USE)
                .addOptions(new OptionData(OptionType.STRING, "component", "component name", false))
                .setAction(this::statusCommand),

            new Command("reload", "reload a component's commands")
                .setRequiredUserRole(UserRole.MANAGE)
                .addOptions(new OptionData(OptionType.STRING, "component", "component name", true))
                .setAction(this::reloadComponent),

            new Command("log", "set the logging channel")
                .setRequiredUserRole(UserRole.MANAGE)
                .addOptions(new OptionData(OptionType.CHANNEL, "channel", "log channel", true)
                    .setChannelTypes(ChannelType.TEXT)
                )
                .setAction(this::setLogChannel),

            new Command("errorlog", "show the error log")
                .setRequiredUserRole(UserRole.MANAGE)
                .setAction(this::showErrorLog),

            new Command("vitals", "show system vitals")
                .setRequiredUserRole(UserRole.MANAGE)
                .setAction(this::getVitals),

            new Command("setmanagerole", "set the role that can manage Eddie Junior")
                .setRequiredUserRole(UserRole.ADMIN)
                .addOptions(new OptionData(OptionType.ROLE, "role", "role", true))
                .setAction(this::setManageRole),

            new Command("setuserole", "set the role that can use Eddie Junior")
                .setRequiredUserRole(UserRole.MANAGE)
                .addOptions(new OptionData(OptionType.ROLE, "role", "role", true))
                .setAction(this::setUseRole),

            new Command("setcommandpermission", "set the user permission of a command")
                .setRequiredUserRole(UserRole.MANAGE)
                .addOptions(
                    new OptionData(OptionType.STRING, "command", "command", true),
                    new OptionData(OptionType.STRING, "permission", "permission", true)
                        .addChoices(
                            Stream.of(UserRole.values())
                                .map(userRole ->
                                    new net.dv8tion.jda.api.interactions.commands.Command.Choice(
                                        userRole.name(), userRole.name())
                                )
                                .toArray(net.dv8tion.jda.api.interactions.commands.Command.Choice[]::new)
                        )
                )
                .setAction(this::setCommandPermission),

            new Command("listpermissions", "list the user permissions of the commands of a component")
                .setRequiredUserRole(UserRole.USE)
                .addOptions(
                    new OptionData(OptionType.STRING, "component", "component", true)
                )
                .setAction(this::listPermissions)

        );
    }

    private void help(@NotNull CommandEvent command, InteractionReply reply) {
        if (command.hasOption("component")) {
            String componentString = command.get("component").getAsString();
            Component component = getComponentSafe(componentString);

            MessageEmbed help;
            if (helpMessages.containsKey(component.getId())) {
                help = helpMessages.get(component.getId());
            } else {
                help = new HelpMessageBuilder(component).build(Colors.TRANSPARENT);
                helpMessages.put(component.getId(), help);
            }

            reply.send(help);
        } else {
            MessageEmbed help;
            if (helpMessages.containsKey(getId())) {
                help = helpMessages.get(getId());
            } else {
                help = new HelpMessageBuilder(this).build(Colors.TRANSPARENT);
                helpMessages.put(getId(), help);
            }

            reply.send(help);
        }
    }

    private void ping(@NotNull CommandEvent command, @NotNull InteractionReply reply) {
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
    }

    private void enableComponent(@NotNull CommandEvent command, InteractionReply reply) {
        String componentString = command.get("component").getAsString();
        Component component = getComponentSafe(componentString);

        if (component instanceof GlobalComponent) {
            throw new BotWarningException("This component is always enabled");
        }

        reply.defer();

        try {
            getServer().toggleComponent(component, true).complete();
            DatabaseManager.toggleComponent(getServer().getId(), componentString, true);
            component.enable();
            reply.send(Icon.ENABLE, "Component `%s` enabled", componentString);
            getServer().log(Colors.BLUE, command.getMember().getUser(),
                "Enabled component `%s`", componentString);
        } catch (Exception e) {
            reply.send(new BotErrorException(e.getMessage()));
            getServer().log(Colors.RED, "Failed to enable component `%s`:%n%s", componentString, e.getMessage());
            getServer().toggleComponent(component, false).queue();
        }
    }

    private void disableComponent(@NotNull CommandEvent command, InteractionReply reply) {
        String componentString = command.get("component").getAsString();

        Component component = getComponentSafe(componentString);

        if (component instanceof GlobalComponent) {
            throw new BotWarningException("This component is always enabled");
        }

        if (component instanceof DirectMessageComponent direct && direct.isRunning()) {
            direct.stop(reply);
        }

        DatabaseManager.toggleComponent(getServer().getId(), componentString, false);
        component.disable();
        getServer().toggleComponent(component, false).queue();
        reply.send(Icon.DISABLE, "Component `%s` disabled", componentString);
        getServer().log(Colors.BLUE, command.getMember().getUser(),
            "Disabled component `%s`", componentString);
    }

    private void listComponents(CommandEvent command, InteractionReply reply) {
        EmbedBuilder embed = new EmbedBuilder()
            .setColor(Colors.TRANSPARENT)
            .setTitle("Components");

        for (Component component : getServer().getComponents()) {
            if (component instanceof GlobalComponent) {
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
                if (component instanceof DirectMessageComponent direct && direct.isRunning()) {
                    builder.append("Listens to DMs\n");
                }
            }

            String title = component.getId().substring(0, 1).toUpperCase(Locale.ROOT)
                + component.getId().substring(1);
            embed.addField(title, builder.toString(), true);
        }

        reply.send(embed.build());
    }

    private void statusCommand(@NotNull CommandEvent command, InteractionReply reply) {
        Component component;

        if (command.hasOption("component")) {
            String componentString = command.get("component").getAsString();
            component = getComponentSafe(componentString);

        } else {
            component = this;
        }

        String title = component.getId().substring(0, 1).toUpperCase(Locale.ROOT)
            + component.getId().substring(1) + " status";

        MessageEmbed embed = new EmbedBuilder()
            .setColor(Colors.TRANSPARENT)
            .setTitle(title)
            .setDescription(component.getStatus())
            .build();

        reply.send(embed);
    }

    private void reloadComponent(@NotNull CommandEvent command, @NotNull InteractionReply reply) {
        Component component = getComponentSafe(command.get("component").getAsString());
        if (component instanceof GlobalComponent) {
            throw new BotErrorException("Cannot reload this component");
        }
        reply.defer();
        try {
            getServer().toggleComponent(component, false).complete();
            getServer().toggleComponent(component, true).complete();
            DatabaseManager.toggleComponent(getServer().getId(), component.getId(), true);
            reply.ok("Reloaded component `%s`", component.getId());
        } catch (Exception e) {
            component.disable();
            DatabaseManager.toggleComponent(getServer().getId(), component.getId(), false);
            reply.send(new BotErrorException(e.getMessage()));
            getServer().log(Colors.RED, "Failed to enable component `%s`:%n%s", component.getId(), e.getMessage());
            getServer().toggleComponent(component, false).queue();
        }
    }

    private void setLogChannel(@NotNull CommandEvent command, @NotNull InteractionReply reply) {
        TextChannel channel = command.get("channel").getAsChannel().asTextChannel();
        PermissionChecker.requireSend(channel);
        getServer().setLog(channel);
        getDatabaseManager().setSetting("log", channel.getId());
        reply.ok("Set logging channel to %s", channel.getAsMention());
    }

    private void showErrorLog(CommandEvent command, InteractionReply reply) {
        File log = Bot.getInstance().getLog();
        if (log == null) {
            throw new BotErrorException("Could not find log file");
        }

        try (FileUpload fileUpload = FileUpload.fromData(log).setName("log.txt")) {
            MessageCreateData message = new MessageCreateBuilder()
                .addFiles(fileUpload)
                .build();
            reply.send(message);
        } catch (IOException e) {
            throw new BotErrorException("Something went wrong");
        }
    }

    private void getVitals(CommandEvent command, @NotNull InteractionReply reply) {
        reply.defer();

        SystemInfo info = new SystemInfo();

        CentralProcessor processor = info.getHardware().getProcessor();
        GlobalMemory memory = info.getHardware().getMemory();

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
                """,
            info.getHardware().getComputerSystem().getModel(),
            info.getOperatingSystem(),
            processor.getProcessorIdentifier().getName().trim(),
            processor.getLogicalProcessorCount(),
            processor.getSystemCpuLoad(1000L) * 100.0,
            processor.getProcessorIdentifier().getVendorFreq() / 1_000_000,
            info.getHardware().getSensors().getCpuTemperature(),
            totalMemory - memory.getAvailable() / 1_048_576,
            totalMemory
        );

        reply.send(new EmbedBuilder()
            .setColor(Colors.TRANSPARENT)
            .setTitle("System Vitals")
            .setDescription(vitals)
            .build());
    }

    private void setManageRole(CommandEvent command, InteractionReply reply) {
        Role role = command.get("role").getAsRole();

        Member member = command.getMember();
        if (getServer().getGuild().getPublicRole().equals(role)) {
            throw new BotWarningException("Cannot set `@everyone` to allow the management of Eddie Junior");
        }

        getServer().setManageRole(role);
        getDatabaseManager().setSetting("managerole", role.getId());
        reply.ok("Enabled role %s to manage Eddie Junior", role.getAsMention());
        getServer().log(Colors.BLUE, member.getUser(), "Enabled role %s (`%s`) to manage Eddie Junior",
            role.getAsMention(), role.getId());
    }

    private void setUseRole(CommandEvent command, InteractionReply reply) {
        Role role = command.get("role").getAsRole();

        Member member = command.getMember();
        if (getServer().getGuild().getPublicRole().equals(role)) {
            throw new BotWarningException("Cannot set `@everyone` to allow the use of Eddie Junior");
        }

        getServer().setUseRole(role);
        getDatabaseManager().setSetting("userole", role.getId());
        reply.ok("Enabled role %s to use Eddie Junior", role.getAsMention());
        getServer().log(Colors.BLUE, member.getUser(), "Enabled role %s (`%s`) to use Eddie Junior",
            role.getAsMention(), role.getId());
    }

    private void setCommandPermission(CommandEvent command, InteractionReply reply) {
        String commandString = command.get("command").getAsString();
        UserRole currentPermission = getServer().getCommandHandler().getRequiredPermission(commandString);
        if (currentPermission == null) {
            throw new BotWarningException("Command not found");
        }

        reply.defer();

        String permission = command.get("permission").getAsString();
        UserRole userRole = UserRole.valueOf(permission);
        getServer().checkPermissions(command.getMember(), userRole);
        getServer().checkPermissions(command.getMember(), currentPermission);

        getServer().getCommandHandler().addRequiredPermission(commandString, userRole);
        getDatabaseManager().removeSetting("commandpermission", commandString + ":" + currentPermission)
            .then(__ -> {
                getDatabaseManager().setSetting("commandpermission", commandString + ":" + userRole).await();
                reply.ok("Set required permission of command `%s` to `%s`", commandString, userRole);
                getServer().log(command.getMember().getUser(), "Set required permission of command `%s` to `%s`",
                    commandString, userRole);
                return null;
            });
    }

    private void listPermissions(CommandEvent command, InteractionReply reply) {
        String componentString = command.get("component").getAsString();
        Component component = getComponentSafe(componentString);
        reply.defer();

        String output = Stream.concat(
                component.getCommands().stream()
                    .flatMap(cmd -> {
                        if (cmd.hasSubCommands()) {
                            return cmd.getSubcommands().stream().map(scmd -> cmd.getName() + " " + scmd.getName());
                        } else {
                            return Stream.of(cmd.getName());
                        }
                    })
                    .map(name -> Map.entry(name,
                        String.valueOf(
                            getServer().getCommandHandler().getRequiredPermission(name)))
                    ),
                Stream.concat(
                    component.getMemberInteractions().stream()
                        .map(interaction -> Map.entry(interaction.getName(),
                            String.valueOf(
                                getServer().getMemberInteractionHandler().getRequiredPermission(interaction.getName())))
                        ),
                    component.getMessageInteractions().stream()
                        .map(interaction -> Map.entry(interaction.getName(),
                            String.valueOf(
                                getServer().getMessageInteractionHandler().getRequiredPermission(interaction.getName())))
                        )
                )
            )
            .sorted(Map.Entry.comparingByKey())
            .map(entry -> String.format("`%s`: `%s`", entry.getKey(), entry.getValue()))
            .collect(Collectors.joining("\n"));

        reply.send(
            new EmbedBuilder()
                .setTitle(componentString)
                .setColor(Colors.TRANSPARENT)
                .setDescription(output)
                .build()
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
            if (component.isEnabled() && !(component instanceof GlobalComponent)) {
                ++count;
            }
        }
        return String.format("""
                Components enabled: %d
                Uptime: %s
                Log: %s
                Manage role: %s
                Use role: %s
                """,
            count,
            Bot.getInstance().getUptime(),
            Optional.ofNullable(getServer().getLog()).map(IMentionable::getAsMention).orElse(null),
            getServer().getManageRole(),
            getServer().getUseRole());
    }

}
