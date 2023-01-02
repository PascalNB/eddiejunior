package com.thefatrat.application.components;

import com.thefatrat.application.entities.Command;
import com.thefatrat.application.exceptions.BotErrorException;
import com.thefatrat.application.exceptions.BotException;
import com.thefatrat.application.exceptions.BotWarningException;
import com.thefatrat.application.reply.Reply;
import com.thefatrat.application.sources.Server;
import com.thefatrat.application.util.Colors;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.ScheduledEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class Event extends Component {

    private final Map<String, Link> links = new HashMap<>();

    public Event(Server server) {
        super(server, "Event", false);

        for (String linkString : getDatabaseManager().getSettings("link")) {
            String[] split = linkString.split("-", 3);
            String keyword = split[0];
            String session = "null".equals(split[1]) ? null : split[1];
            String component = "null".equals(split[2]) ? null : split[2];
            Link link = new Link(keyword, session, component);
            links.put(keyword, link);
        }

        getServer().getEventHandler().addListener((event, __) -> {
            if (!isEnabled() || !event.getStatus().equals(ScheduledEvent.Status.ACTIVE)
                && !event.getStatus().equals(ScheduledEvent.Status.COMPLETED)) {
                return;
            }

            String name = event.getName();
            Link link = null;
            for (Link l : links.values()) {
                if (name.toLowerCase(Locale.ROOT).contains(l.keyword())) {
                    link = l;
                    break;
                }
            }
            if (link == null) {
                return;
            }

            Session sessionComponent = null;
            if (link.session() != null) {
                sessionComponent = getServer().getComponent(Session.NAME, Session.class);
            }
            RunnableComponent component = null;
            if (link.component() != null) {
                Component c = getServer().getComponent(link.component());
                if (c != null && c.isEnabled() && c instanceof RunnableComponent dc && !dc.isAutoRunnable()) {
                    component = dc;
                }
            }

            if (event.getStatus().equals(ScheduledEvent.Status.ACTIVE)) {
                if (link.session() != null && sessionComponent != null && sessionComponent.isEnabled()
                    && sessionComponent.isSession(link.session())) {
                    try {
                        sessionComponent.openSession(link.session(), Reply.EMPTY);
                    } catch (BotException ignore) {
                    }
                }

                if (link.component() != null && component != null && !component.isRunning()) {
                    component.start(Reply.EMPTY);
                }

            } else if (event.getStatus().equals(ScheduledEvent.Status.COMPLETED)) {
                if (link.session() != null && sessionComponent != null && sessionComponent.isEnabled()
                    && sessionComponent.isSession(link.session())) {
                    try {
                        sessionComponent.closeSession(link.session(), Reply.EMPTY);
                    } catch (BotException ignore) {
                    }
                }

                if (link.component() != null && component != null) {
                    component.stop(Reply.EMPTY);
                }
            }
        });

        setComponentCommand();

        addSubcommands(
            new Command("link", "link a session to a keyword in an event name")
                .addOptions(
                    new OptionData(OptionType.STRING, "keyword", "event name keyword", true),
                    new OptionData(OptionType.STRING, "session", "session", false),
                    new OptionData(OptionType.STRING, "component", "runnable component", false)
                )
                .setAction((command, reply) -> {
                    OptionMapping sessionObject = command.getArgs().get("session");
                    OptionMapping componentObject = command.getArgs().get("component");
                    if (sessionObject == null && componentObject == null) {
                        throw new BotErrorException("Please provide a session or runnable component");
                    }

                    String keyword = command.getArgs().get("keyword").getAsString().toLowerCase(Locale.ROOT);
                    if (links.containsKey(keyword)) {
                        throw new BotWarningException("`%s` is already linked, unlink it first", keyword);
                    }

                    String session = null;
                    if (sessionObject != null) {
                        session = sessionObject.getAsString();
                        Session sessionComponent = getServer().getComponent(Session.NAME, Session.class);
                        if (sessionComponent != null && !sessionComponent.isSession(session)) {
                            throw new BotErrorException(Session.ERROR_SESSION_NONEXISTENT);
                        }
                    }

                    String component = null;
                    if (componentObject != null) {
                        component = componentObject.getAsString();
                        Component componentInstance = getServer().getComponent(component);
                        if (componentInstance == null) {
                            throw new BotErrorException("Component `%s` does not exist", component);
                        }
                        if (componentInstance instanceof RunnableComponent directComponent) {
                            if (directComponent.isAutoRunnable()) {
                                throw new BotErrorException("Component `%s` is not runnable", component);
                            }
                        } else {
                            throw new BotErrorException("Component `%s` is not runnable", component);
                        }
                    }

                    Link link = new Link(keyword, session, component);
                    getDatabaseManager().addSetting("link", link.toString());
                    links.put(keyword, link);
                    reply.ok("Linked keyword `%s`", keyword);
                }),

            new Command("unlink", "unlink a keyword")
                .addOption(new OptionData(OptionType.STRING, "keyword", "event name keyword", true))
                .setAction((command, reply) -> {
                    String keyword = command.getArgs().get("keyword").getAsString().toLowerCase(Locale.ROOT);

                    if (!links.containsKey(keyword)) {
                        throw new BotErrorException("`%s` is not a linked keyword", keyword);
                    }

                    Link link = links.remove(keyword);
                    getDatabaseManager().removeSetting("link", link.toString());
                    reply.ok("Unlinked keyword `%s`", keyword);
                }),

            new Command("list", "show the list of links")
                .setAction((command, reply) -> {
                    if (links.isEmpty()) {
                        throw new BotWarningException("No links have been added yet");
                    }

                    String[] array = new String[links.size()];

                    int i = 0;
                    for (Link link : links.values()) {
                        array[i] = String.format("Keyword: `%s`%nSession: `%s`%nComponent: `%s`",
                            link.keyword(), link.session(), link.component());
                        ++i;
                    }

                    String joined = String.join("\n\n", array);
                    reply.accept(new EmbedBuilder()
                        .setColor(Colors.TRANSPARENT)
                        .setTitle("List of links")
                        .setDescription(joined)
                        .build());
                })
        );
    }

    private record Link(String keyword, @Nullable String session, @Nullable String component) {

        @Override
        public String toString() {
            return String.format("%s-%s-%s", keyword, session, component);
        }

    }

}
