package com.thefatrat.eddiejunior;

import com.thefatrat.eddiejunior.components.*;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import java.io.*;
import java.util.Properties;

public class Initializer {

    private static final Initializer instance = new Initializer();

    private final Properties properties = new Properties();

    public static Initializer getInstance() {
        return instance;
    }

    private Initializer() {
        try {
            File jarPath = new File(Initializer.class.getProtectionDomain()
                .getCodeSource().getLocation().getPath());
            String propertiesPath = jarPath.getParentFile().getAbsolutePath();
            properties.load(new FileInputStream(propertiesPath + "/config.cfg"));

        } catch (FileNotFoundException e) {
            try (InputStream config = Initializer.class.getClassLoader().getResourceAsStream("config.cfg")) {
                properties.load(config);
            } catch (IOException e2) {
                throw new UncheckedIOException(e2);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void main(String[] args) {
        final String token = getInstance().getProperty("bot_token");
        final JDA jda;

        Bot.getInstance().setComponents(
            Manager.class,

            ModMail.class,
            Feedback.class,

            PollComponent.class,
            Session.class,
            Event.class,

            Grab.class,
            Roles.class,
            Hoist.class,

            MessageComponent.class
        );

        jda = JDABuilder.createLight(token,
                GatewayIntent.DIRECT_MESSAGES,
                GatewayIntent.GUILD_MEMBERS,
                GatewayIntent.MESSAGE_CONTENT,
                GatewayIntent.SCHEDULED_EVENTS
            )
            .enableCache(CacheFlag.MEMBER_OVERRIDES, CacheFlag.SCHEDULED_EVENTS)
            .setRawEventsEnabled(false)
            .addEventListeners(Bot.getInstance())
            .build();

        Bot.getInstance().setJDA(jda);
        jda.getPresence().setPresence(OnlineStatus.IDLE, Activity.playing("Starting..."));

        try {
            jda.awaitReady();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public String getProperty(String property) {
        return properties.getProperty(property);
    }

}