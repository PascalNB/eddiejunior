package com.thefatrat.database;

import java.util.regex.Matcher;

/**
 * Class that represents a database query with options to insert values.
 */
public class Query {

    private final String query;
    private final Object[] args;

    /**
     * Creates a new query object for the given query string.
     *
     * @param query the query
     */
    private Query(String query, Object... args) {
        this.query = query;
        this.args = args;
    }

    public static Query of(String query, Object... args) {
        return new Query(query, args);
    }

    /**
     * Fills in all {@code ?} of the query with the given variables.
     *
     * @param variables the variables
     * @return the query with the variables
     */
    public Query withVariables(Object... variables) {
        String query = this.query;

        for (Object variable : variables) {
            query = query.replaceFirst("\\?",
                Matcher.quoteReplacement(variable.toString()));
        }

        return Query.of(query);
    }

    @Override
    public String toString() {
        return query;
    }

    public Object[] getArgs() {
        return args;
    }

}