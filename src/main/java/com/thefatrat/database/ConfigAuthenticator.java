package com.thefatrat.database;

import com.thefatrat.application.Initializer;

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
        Initializer initializer = Initializer.getInstance();
        String username = initializer.getProperty("username");
        String password = initializer.getProperty("password");
        String url = initializer.getProperty("host");

        try {
            Class.forName(initializer.getProperty("driver"));
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            throw new DatabaseException("JDBC driver not loaded");
        }

        return new String[]{username, password, url};
    }

}
