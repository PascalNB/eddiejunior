package com.thefatrat.database;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

public class DatabaseAction<T> {

    private static final Executor EXECUTOR = command -> new Thread(command).start();
    private final Query query;

    public DatabaseAction(Query query) {
        this.query = query;
    }

    public DatabaseAction(String query, Object... args) {
        this(Query.of(query, args));
    }

    public CompletableFuture<T> queue(Function<Table, T> callback) {
        return CompletableFuture.supplyAsync(() -> {
            AtomicReference<T> reference = new AtomicReference<>();
            Database database = Database.getInstance().connect();
            try {
                database.queryStatement(table -> reference.set(callback.apply(table)), query);
            } finally {
                database.close();
            }
            return reference.get();
        }, EXECUTOR);
    }

    public CompletableFuture<Void> queue(Consumer<Table> callback) {
        return CompletableFuture.runAsync(() -> {
            Database database = Database.getInstance().connect();
            try {
                database.queryStatement(callback, query);
            } finally {
                database.close();
            }
        }, EXECUTOR);
    }

    public CompletableFuture<Void> execute() {
        return CompletableFuture.runAsync(() -> {
            Database database = Database.getInstance().connect();
            try {
                database.executeStatement(query);
            } finally {
                database.close();
            }
        }, EXECUTOR);

    }

}

