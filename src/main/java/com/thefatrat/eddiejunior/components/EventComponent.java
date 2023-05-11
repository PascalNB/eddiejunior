package com.thefatrat.eddiejunior.components;

import com.thefatrat.eddiejunior.entities.Command;
import com.thefatrat.eddiejunior.exceptions.BotErrorException;
import com.thefatrat.eddiejunior.exceptions.BotException;
import com.thefatrat.eddiejunior.exceptions.BotWarningException;
import com.thefatrat.eddiejunior.reply.Reply;
import com.thefatrat.eddiejunior.sources.Server;
import com.thefatrat.eddiejunior.util.Colors;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.ScheduledEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class EventComponent extends Component {

    private final Map<String, Link> links = new HashMap<>();

    public EventComponent(Server server) {
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

            String name = event.getName().toLowerCase(Locale.ROOT);
            Link link = null;

            for (Link l : links.values()) {
                if (name.contains(l.keyword())
                    && (link == null || link.keyword().length() < l.keyword().length())) {
                    link = l;
                }
            }
            if (link == null) {
                return;
            }

            SessionComponent sessionComponent = null;
            if (link.session() != null) {
                sessionComponent = getServer().getComponent(SessionComponent.NAME, SessionComponent.class);
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
                        getServer().log(Colors.GREEN, "Opened session `%s`", link.session());
                        sessionComponent.openSession(link.session(), Reply.EMPTY);
                    } catch (BotException ignore) {
                    }
                }

                if (link.component() != null && component != null && !component.isRunning()) {
                    component.start(Reply.EMPTY);
                    getServer().log(Colors.GREEN, "Component `%s` started running", link.component());
                }

            } else if (event.getStatus().equals(ScheduledEvent.Status.COMPLETED)) {
                if (link.session() != null && sessionComponent != null && sessionComponent.isEnabled()
                    && sessionComponent.isSession(link.session())) {
                    try {
                        getServer().log(Colors.RED, "Closed session `%s`", link.session());
                        sessionComponent.closeSession(link.session(), Reply.EMPTY);
                    } catch (BotException ignore) {
                    }
                }

                if (link.component() != null && component != null) {
                    component.stop(Reply.EMPTY);
                    getServer().log(Colors.RED, "Component `%s` stopped running", link.component());
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
                    OptionMapping sessionObject = command.get("session");
                    OptionMapping componentObject = command.get("component");
                    if (sessionObject == null && componentObject == null) {
                        throw new BotErrorException("Please provide a session or runnable component");
                    }

                    String keyword = command.get("keyword").getAsString().toLowerCase(Locale.ROOT);
                    if (links.containsKey(keyword)) {
                        throw new BotWarningException("`%s` is already linked, unlink it first", keyword);
                    }

                    String session = null;
                    if (sessionObject != null) {
                        session = sessionObject.getAsString();
                        SessionComponent sessionComponent = getServer().getComponent(SessionComponent.NAME,
                            SessionComponent.class);
                        if (sessionComponent != null && !sessionComponent.isSession(session)) {
                            throw new BotErrorException(SessionComponent.ERROR_SESSION_NONEXISTENT);
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
                .addOptions(new OptionData(OptionType.STRING, "keyword", "event name keyword", true))
                .setAction((command, reply) -> {
                    String keyword = command.get("keyword").getAsString().toLowerCase(Locale.ROOT);

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
                    reply.send(new EmbedBuilder()
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