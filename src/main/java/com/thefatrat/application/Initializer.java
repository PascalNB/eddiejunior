package com.thefatrat.application;

import com.thefatrat.database.DatabaseAuthenticator;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Objects;
import java.util.Properties;

public class Initializer {

    public static void main(String[] args) {
        DatabaseAuthenticator.getInstance().authenticate();

        final String token = getToken();
        final JDA jda;

        try {
            jda = JDABuilder.createLight(token)
                .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                .addEventListeners(new Bot())
                .build();
        } catch (LoginException e) {
            throw new RuntimeException(e);
        }

        try {
            jda.awaitReady();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getToken() {
        try (InputStream config = Initializer.class.getClassLoader()
            .getResourceAsStream("config.cfg")) {

            Properties properties = new Properties();
            properties.load(config);

            return Objects.requireNonNull(properties.getProperty("bot_token"));

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}