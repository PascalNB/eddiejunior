package com.thefatrat.database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * The JDBC implementation of {@link Database}.
 */
@SuppressWarnings("unused")
public class JDBC extends Database {

    private static final int FETCH_SIZE = 500;

    private Connection connection = null;
    private Statement statement = null;

    @Override
    public Database connect() {
        if (url == null) {
            throw new DatabaseException("URL for database connection not set.");
        }

        try {
            connection = DriverManager.getConnection(url, username, password);
            connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            connection.setAutoCommit(false);
            statement = connection.createStatement();
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }

        return this;
    }

    @Override
    protected void checkConnection() throws DatabaseException {
        if (connection == null || statement == null) {
            throw new DatabaseException("No connection to the database exists" +
                "or it has already been closed.");
        }
    }

    @Override
    public Database execute(Query query) {
        checkConnection();

        try {
            statement.execute(query.toString());
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }

        return this;
    }

    @Override
    public Database query(Consumer<Table> callback, Query query) {
        checkConnection();

        try {
            statement.setFetchSize(FETCH_SIZE);

            callback.accept(parseResult(statement.executeQuery(query.toString())));
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }

        return this;
    }

    private PreparedStatement setVariables(PreparedStatement statement, Object... variables) {
        try {
            statement.setFetchSize(FETCH_SIZE);

            for (int i = 0; i < variables.length; i++) {
                Object variable = variables[i];
                int index = i + 1;

                if (variable instanceof Integer) {
                    statement.setInt(index, (int) variable);
                } else if (variable instanceof Double) {
                    statement.setDouble(index, (double) variable);
                } else if (variable instanceof Boolean) {
                    statement.setBoolean(index, (boolean) variable);
                } else {
                    statement.setString(index, (String) variable);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
        return statement;
    }

    @Override
    public Database queryStatement(Consumer<Table> callback, Query statement,
        Object... variables) {
        checkConnection();

        try {
            callback.accept(
                parseResult(setVariables(
                    connection.prepareStatement(statement.toString()),
                    variables
                ).executeQuery()));
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }

        return this;
    }

    @Override
    public Database executeStatement(Query statement, Object... variables) {
        checkConnection();

        try {
            setVariables(connection.prepareStatement(statement.toString()), variables).execute();
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }

        return this;
    }

    public Database executeBatch(String preparedStatement, Object[]... variablesArray) {
        checkConnection();

        try {
            PreparedStatement statement = connection.prepareStatement(preparedStatement);

            for (Object[] variables : variablesArray) {
                setVariables(statement, variables).addBatch();
            }

            statement.executeBatch();

        } catch (SQLException e) {
            throw new DatabaseException(e);
        }

        return this;
    }

    @Override
    public void close() {
        checkConnection();

        try {
            connection.commit();
            connection.setAutoCommit(true);
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        connection = null;
        statement = null;
    }

    // specific implementation to parse a ResultSet to a Table
    private static Table parseResult(ResultSet resultSet) {
        String[] attributes;
        List<Tuple> tuples = new ArrayList<>();
        Table table;
        try {
            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();
            attributes = new String[columnCount];
            for (int i = 0; i < columnCount; i++) {
                attributes[i] = metaData.getColumnName(i + 1);
            }

            table = new Table(attributes);

            while (resultSet.next()) {
                String[] tuple = new String[columnCount];
                for (int i = 0; i < columnCount; i++) {
                    tuple[i] = resultSet.getString(i + 1);
                }
                table.addRow(tuple);
            }

        } catch (SQLException e) {
            throw new DatabaseException(e);
        }

        return table;
    }

}
