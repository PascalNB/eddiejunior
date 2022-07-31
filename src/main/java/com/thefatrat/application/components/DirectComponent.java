package com.thefatrat.application.components;

import com.thefatrat.application.Command;
import com.thefatrat.application.PermissionChecker;
import com.thefatrat.application.handlers.CommandHandler;
import com.thefatrat.application.sources.Server;
import com.thefatrat.application.sources.Source;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class DirectComponent extends Component {

    private final CommandHandler handler = new CommandHandler();
    private boolean running = false;
    private boolean paused = false;
    private MessageChannel destination;

    public DirectComponent(Source server, String name) {
        super(server, name, false);
    }

    @Override
    public void disable() {
        super.disable();
    }

    @Override
    public void register() {
        getSource().getCommandHandler().addListener(getName(), command -> {
            if (!isEnabled() || command.args().length == 0) {
                return;
            }
            String newCommand = command.args()[0];
            String[] newArgs = command.args().length > 1
                ? Arrays.copyOfRange(command.args(), 1, command.args().length)
                : new String[0];

            handler.handle(new Command(newCommand, newArgs, command.message(), command.member()));
        }, PermissionChecker.IS_ADMIN);

        ((Server) getSource()).getDirectHandler().addListener(message -> {
            if (!isRunning() || isPaused() || getDestination() == null) {
                return;
            }
            handleDirect(message);
        });

        handler.addListener("start", command -> {
            MessageChannel parsedDestination = parseDestination(command, 0);
            MessageChannel newDestination;

            if (parsedDestination != null) {
                newDestination = parsedDestination;
            } else {
                newDestination = getDestination() == null
                    ? command.message().getChannel()
                    : getDestination();
            }

            if (getDestination() == null ||
                !newDestination.getId().equals(getDestination().getId())) {
                setDestination(newDestination);

                command.message().getChannel().sendMessageFormat(
                    ":gear: Destination set to %s `(%s)`%n",
                    getDestination().getAsMention(), getDestination().getId()
                ).queue();
            }

            List<DirectComponent> pausedComponents = new ArrayList<>();
            for (DirectComponent component : getSource().getDirectComponents()) {
                if (component != this && component.isRunning() && !component.isPaused()) {
                    component.setPaused(true);
                    pausedComponents.add(component);
                }
            }
            if (pausedComponents.size() > 0) {
                StringBuilder builder = new StringBuilder()
                    .append(":pause_button: The following components were paused:");
                for (Component component : pausedComponents) {
                    builder.append(" ").append(component.getName()).append(",");
                }
                builder.deleteCharAt(builder.length() - 1);
                command.message().getChannel().sendMessage(builder.toString()).queue();
            }

            start(command);
            setPaused(false);
            setRunning(true);
        });

        handler.addListener("stop", command -> {
            setRunning(false);
            stop(command);
        });

        handler.addListener("destination", command -> {
            MessageChannel newDestination;
            if (command.args().length == 0) {
                newDestination = command.message().getChannel();
            } else {
                newDestination = parseDestination(command, 0);
            }

            if (newDestination != null) {
                setDestination(newDestination);
                command.message().getChannel().sendMessageFormat(
                        ":gear: Destination set to %s `(%s)`%n",
                        getDestination().getAsMention(), getDestination().getId())
                    .queue();
                return;
            }

            command.message().getChannel().sendMessageFormat(
                ":x: The given destination channel was not found."
            ).queue();
        });
    }

    private MessageChannel parseDestination(Command command, int position) {
        MessageChannel result;
        List<GuildChannel> list = command.message().getMentions().getChannels();

        if (list.size() != 0) {
            String id = list.get(0).getId();
            result = command.message()
                .getGuild().getChannelById(MessageChannel.class, id);

        } else {
            if (command.args().length == 0) {
                return null;
            }
            result = command.message().getGuild()
                .getChannelById(MessageChannel.class, command.args()[position]);
        }

        return result;
    }

    protected abstract void handleDirect(Message message);

    protected abstract void stop(Command command);

    protected abstract void start(Command command);

    protected CommandHandler getHandler() {
        return handler;
    }

    public void setDestination(MessageChannel destination) {
        this.destination = destination;
    }

    public MessageChannel getDestination() {
        return destination;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public boolean isRunning() {
        return running && isEnabled();
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public boolean isPaused() {
        return paused;
    }

}
