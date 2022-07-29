package com.thefatrat.database;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Implementation of {@link DatabaseAuthenticator} that reads the credentials from a cfg file.
 *
 * @see DatabaseAuthenticator
 */
@SuppressWarnings("unused")
public class ConfigAuthenticator extends DatabaseAuthenticator {

    /**
     * Returns the database credentials from the credentials file.
     */
    public String[] getCredentials() {
        try (InputStream file = ConfigAuthenticator.class.getClassLoader()
            .getResourceAsStream("config.cfg")) {

            Properties properties = new Properties();
            properties.load(file);

            String username = properties.getProperty("username", "");
            String password = properties.getProperty("password", "");
            String url = properties.getProperty("host");

            try {
                Class.forName(properties.getProperty("driver"));
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                throw new DatabaseException("JDBC driver not loaded");
            }

            return new String[]{username, password, url};

        } catch (IOException e) {
            return null;
        }
    }

}
