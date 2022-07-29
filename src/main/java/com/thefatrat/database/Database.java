package com.thefatrat.database;

import java.lang.reflect.InvocationTargetException;
import java.util.function.Consumer;

/**
 * Abstract class that specifies all the methods needed for a database connection.
 */
public abstract class Database {

    private static final Class<? extends Database> implementation = JDBC.class;

    protected static String url = null;
    protected static String username = null;
    protected static String password = null;

    /**
     * Sets the connection URL for all database connections.
     * Example for jdbc: jdbc:postgresql://example.com:5432/
     *
     * @param url the connection URL
     */
    public static void setUrl(String url) {
        Database.url = url;
    }

    /**
     * Sets the username for all database connections
     *
     * @param username the username
     */
    public static void setUsername(String username) {
        Database.username = username;
    }

    /**
     * Sets the password for all database connections.
     *
     * @param password the password
     */
    public static void setPassword(String password) {
        Database.password = password;
    }

    /**
     * @return an instance of database.Database based on the implementation
     */
    public static Database getInstance() {
        try {
            return implementation.getDeclaredConstructor().newInstance();
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException |
                 InvocationTargetException e) {
            throw new DatabaseException(e);
        }
    }

    /**
     * Prints a Table in readable form to the given output.
     *
     * @param table the query result
     */
    public static void printQueryResult(Table table) {
        StringBuilder result = new StringBuilder();

        String[] attributes = table.getAttributes();
        for (int i = 0; i < table.getColumnCount(); i++) {
            result.append(attributes[i]).append(", ");
        }
        result.delete(result.length() - 2, result.length());
        result.append("\n");

        // empty resultSet
        if (table.getColumnCount() == 0) {
            System.out.println("Number of rows: 0");
            return;
        }

        table.forEach(row -> {
            for (int i = 0; i < table.getColumnCount(); i++) {
                result.append(row.get(i)).append(", ");
            }
            result.delete(result.length() - 2, result.length());
            result.append("\n");
        });

        System.out.println(result);
        System.out.println("Number of rows: " + table.getRowCount());
    }

    /**
     * Connects the database.Database object to the database.
     *
     * @return the same {@link Database}
     */
    public abstract Database connect();

    /**
     * @throws DatabaseException if this {@link Database} instance is not connected to a database.
     */
    protected abstract void checkConnection() throws DatabaseException;

    /**
     * Executes a query without receiving data back.
     *
     * @param query the query to be executed
     * @return the same {@link Database}
     */
    public abstract Database execute(Query query);

    /**
     * Executes an SQL query on the database.
     *
     * @param callback the consumer that accepts the result from the database
     * @param query    the query
     * @return the same {@link Database}
     */
    public abstract Database query(Consumer<Table> callback, Query query);

    /**
     * Queries a prepared statement on the database.
     *
     * @param callback          the consumer that accepts the result from the database
     * @param preparedStatement the prepared query
     * @param variables         the variables that should be added to the query
     * @return the same {@link Database}
     */
    public abstract Database queryStatement(Consumer<Table> callback, Query preparedStatement,
        Object... variables);

    /**
     * Executes a prepared statement on the database.
     *
     * @param preparedStatement the prepared statement
     * @param variables         the variables that should be added to the query
     * @return the same {@link Database}
     */
    public abstract Database executeStatement(Query preparedStatement, Object... variables);

    /**
     * Closes the connection to the database.
     */
    public abstract void close();

}
