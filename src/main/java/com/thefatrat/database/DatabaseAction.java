package com.thefatrat.database;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

public class DatabaseAction<T> {

    private final Query query;
    private final Object[] args;

    public DatabaseAction(Query query, Object... args) {
        this.query = query;
        this.args = args;
    }

    public DatabaseAction(String query, Object... args) {
        this(Query.of(query), args);
    }

    public CompletableFuture<T> queue(Function<Table, T> callback) {
        return CompletableFuture.supplyAsync(() -> {
            AtomicReference<T> reference = new AtomicReference<>();
            Database database = Database.getInstance().connect();
            try {
                database.queryStatement(table -> reference.set(callback.apply(table)), query, args);
            } finally {
                database.close();
            }
            return reference.get();
        });
    }

    public CompletableFuture<Void> queue(Consumer<Table> callback) {
        return CompletableFuture.runAsync(() -> {
            Database database = Database.getInstance().connect();
            try {
                database.queryStatement(callback, query, args);
            } finally {
                database.close();
            }
        });
    }

    public CompletableFuture<Void> execute() {
        return CompletableFuture.runAsync(() -> {
            Database database = Database.getInstance().connect();
            try {
                database.executeStatement(query, args);
            } finally {
                database.close();
            }
        });

    }

}

