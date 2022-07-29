package com.thefatrat.database;

/**
 * Abstract singleton that is used to get the database credentials and set up the database
 * connection. Requires an implementation that extends {@link DatabaseAuthenticator}.
 */
public abstract class DatabaseAuthenticator {

    private static DatabaseAuthenticator instance;

    public static DatabaseAuthenticator getInstance() {
        if (instance == null) {
            instance = new ConfigAuthenticator();
        }
        return instance;
    }

    /**
     * Sets the username, password and url for all database connections.
     *
     * @throws DatabaseException when a database error occurs
     */
    public void authenticate() throws DatabaseException {
        String[] credentials = getCredentials();

        Database.setUsername(credentials[0]);
        Database.setPassword(credentials[1]);
        Database.setUrl(credentials[2]);

        // test connection
        Database.getInstance().connect().close();
    }

    /**
     * Returns the username, password and connection url in an array.
     *
     * @return the credentials
     */
    public abstract String[] getCredentials();

}