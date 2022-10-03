package com.thefatrat.application;

import com.thefatrat.application.components.Feedback;
import com.thefatrat.application.components.Manager;
import com.thefatrat.application.components.ModMail;
import com.thefatrat.database.DatabaseAuthenticator;
import com.thefatrat.database.DatabaseException;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;

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
        try {
            DatabaseAuthenticator.getInstance().authenticate();
        } catch (DatabaseException e) {
            e.printStackTrace();
            DatabaseManager.setAccessible(false);
        }

        final String token = getInstance().getProperty("bot_token");
        final JDA jda;

        jda = JDABuilder.createLight(token,
                GatewayIntent.DIRECT_MESSAGES
            )
            .setMemberCachePolicy(MemberCachePolicy.NONE)
            .setChunkingFilter(ChunkingFilter.NONE)
            .addEventListeners(Bot.getInstance())
            .build();

        Bot.getInstance().setJDA(jda);
        Bot.getInstance().setComponents(
            Manager.class,
            ModMail.class,
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