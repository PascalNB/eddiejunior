package com.thefatrat.eddiejunior;

import com.thefatrat.eddiejunior.components.impl.*;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import java.io.*;
import java.nio.file.Path;
import java.util.Properties;

public class Initializer {

    private static final Initializer instance = new Initializer();

    private final Properties properties = new Properties();

    public static Initializer getInstance() {
        return instance;
    }

    private Initializer() {
        try {
            File jarPath = new File(Initializer.class.getProtectionDomain().getCodeSource().getLocation().getPath());
            Path propertiesPath = jarPath.getParentFile().toPath().toAbsolutePath();
            File configFile = propertiesPath.resolve("config.cfg").toFile();
            properties.load(new FileInputStream(configFile));

        } catch (FileNotFoundException __) {
            try (InputStream config = Initializer.class.getClassLoader().getResourceAsStream("config.cfg")) {
                properties.load(config);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public String getProperty(String property) {
        return properties.getProperty(property);
    }

    public static void main(String[] args) {
        final String token = getInstance().getProperty("bot_token");
        final String logFilePath = getInstance().getProperty("log_file");
        final JDA jda;

        Bot.getInstance().setComponents(
            ManagerComponent.class,
            ModMailComponent.class,
            FeedbackComponent.class,
            FanMailComponent.class,
            FaqComponent.class,
            PollComponent.class,
            SessionComponent.class,
            EventComponent.class,
            ChannelComponent.class,
            GrabComponent.class,
            RoleComponent.class,
            StickerComponent.class,
            HoistComponent.class,
            MessageComponent.class,
            NicknameComponent.class,
            ForwardPurgeComponent.class
        );

        jda = JDABuilder.createLight(token,
                GatewayIntent.DIRECT_MESSAGES,
                GatewayIntent.GUILD_MEMBERS,
                GatewayIntent.MESSAGE_CONTENT,
                GatewayIntent.SCHEDULED_EVENTS,
                GatewayIntent.GUILD_MESSAGES
            )
            .setMemberCachePolicy(MemberCachePolicy.NONE)
            .enableCache(
                CacheFlag.MEMBER_OVERRIDES,
                CacheFlag.SCHEDULED_EVENTS,
                CacheFlag.ROLE_TAGS)
            .setRawEventsEnabled(false)
            .setEventPassthrough(false)
            .addEventListeners(Bot.getInstance())
            .build();

        Bot.getInstance().setJDA(jda);

        if (logFilePath != null) {
            File logFile = new File(logFilePath);
            if (logFile.exists()) {
                Bot.getInstance().setLog(logFile);
            }
        }

        jda.getPresence().setPresence(OnlineStatus.IDLE, Activity.playing("Starting..."));

        try {
            jda.awaitReady();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}