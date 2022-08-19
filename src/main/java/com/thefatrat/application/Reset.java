package com.thefatrat.application;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;

import javax.security.auth.login.LoginException;
import java.io.*;
import java.util.Properties;

public class Reset {

    private final Properties properties = new Properties();

    private Reset() {
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

    public String getProperty(String property) {
        return properties.getProperty(property);
    }

    public static void main(String[] args) {
        final String token = new Reset().getProperty("bot_token");
        final JDA jda;

        try {
            jda = JDABuilder.createLight(token)
                .setMemberCachePolicy(MemberCachePolicy.NONE)
                .setChunkingFilter(ChunkingFilter.NONE)
                .build();
        } catch (LoginException e) {
            throw new RuntimeException(e);
        }

        try {
            jda.awaitReady();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        RestAction.allOf(jda.getGuilds().stream().map(Guild::updateCommands).toList()).queue();

        jda.shutdown();
    }

}
