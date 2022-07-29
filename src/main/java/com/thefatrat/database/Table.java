package com.thefatrat.database;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * All query consumers accept this data type.
 * Any implementation of Database should create a Table for each query.
 */
public class Table {

    private final String[] attributes;
    private final List<Tuple> tuples;
    private final Map<String, Integer> index;

    public Table(String... attributes) {
        this(attributes, new ArrayList<>());
    }

    /**
     * Creates a new query result with the given attribute names and values.
     *
     * @param attributes the names of the attributes
     * @param tuples     the tuples with values
     */
    public Table(String[] attributes, List<String[]> tuples) {
        this.attributes = attributes;
        this.tuples = new ArrayList<>();
        index = new HashMap<>();
        for (int i = 0; i < attributes.length; i++) {
            index.put(attributes[i], i);
        }
        tuples.forEach(t -> this.tuples.add(new Row(t)));
    }

    public Table addRow(String... cells) {
        tuples.add(new Row(cells));
        return this;
    }

    /**
     * @return the names of the attributes
     */
    public String[] getAttributes() {
        return attributes;
    }

    /**
     * @param index the index of the column
     * @return the attribute name of the column
     */
    public String getAttribute(int index) {
        return attributes[index];
    }

    /**
     * Returns a column of the given index.
     *
     * @param index the index of the column
     * @return the column
     */
    public String[] getColumn(int index) {
        String[] result = new String[getRowCount()];

        for (int i = 0; i < result.length; i++) {
            result[i] = tuples.get(i).get(index);
        }

        return result;
    }

    /**
     * Returns the rowindex of the given attribute name.
     * Returns -1 when the attribute name is not found in the table.
     *
     * @param attributeName the attribute name
     * @return the index
     */
    public int indexOf(String attributeName) {
        return index.getOrDefault(attributeName, -1);
    }

    /**
     * Returns a column with the given attribute name.
     *
     * @param attributeName the name of the column
     * @return the column
     */
    public String[] getColumn(String attributeName) {
        int index = indexOf(attributeName);

        if (index == -1) {
            return new String[0];
        }

        return getColumn(index);
    }

    /**
     * Loops over each row and executes the given function.
     *
     * @param consumer the consumer that accepts the row
     */
    public void forEach(Consumer<Tuple> consumer) {
        for (Tuple tuple : tuples) {
            consumer.accept(tuple);
        }
    }

    /**
     * Returns a list of all tuples in the table.
     *
     * @return a list of tuples
     */
    public List<Tuple> getTuples() {
        return tuples;
    }

    /**
     * Returns the row for the given row index.
     *
     * @param index the index of the row
     * @return an array with the row values
     */
    public Tuple getRow(int index) {
        return tuples.get(index);
    }

    /**
     * @return the row count
     */
    public int getRowCount() {
        return tuples.size();
    }

    /**
     * @return the column count
     */
    public int getColumnCount() {
        return attributes.length;
    }

    private class Row implements Tuple {

        private final String[] cells;

        public Row(String... cells) {
            this.cells = cells;
        }

        @Override
        public String get(String attribute) {
            int i = indexOf(attribute);
            return i == -1 ? null : cells[i];
        }

        @Override
        public String get(int i) {
            return cells[i];
        }

    }

}
