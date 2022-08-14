package com.thefatrat.application;

import com.thefatrat.application.components.Feedback;
import com.thefatrat.application.components.Manager;
import com.thefatrat.database.DatabaseAuthenticator;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;

import javax.security.auth.login.LoginException;
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
            try (InputStream config = Initializer.class.getClassLoader()
                .getResourceAsStream("config.cfg")) {
                properties.load(config);
            } catch (IOException e2) {
                throw new UncheckedIOException(e2);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void main(String[] args) {
        DatabaseAuthenticator.getInstance().authenticate();

        final String token = getInstance().getProperty("bot_token");
        final JDA jda;

        try {
            jda = JDABuilder.createDefault(token)
                .enableIntents(
                    GatewayIntent.MESSAGE_CONTENT,
                    GatewayIntent.GUILD_MEMBERS,
                    GatewayIntent.DIRECT_MESSAGES,
                    GatewayIntent.GUILD_PRESENCES
                )
                .setChunkingFilter(ChunkingFilter.ALL)
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .addEventListeners(Bot.getInstance())
                .build();
        } catch (LoginException e) {
            throw new RuntimeException(e);
        }

        Bot.getInstance().setJDA(jda);
        Bot.getInstance().setComponents(
            Manager.class,
//            ModMail.class,
            Feedback.class
        );

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