package com.thefatrat.database;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
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

    private static <T> CompletableFuture<T> queue(DatabaseAction<?> action, Function<Table, T> mapper) {
        return CompletableFuture.supplyAsync(() -> {
            AtomicReference<T> reference = new AtomicReference<>();
            Database database = Database.getInstance().connect();
            try {
                database.queryStatement(table -> reference.set(mapper.apply(table)), action.query);
                return reference.get();
            } finally {
                database.close();
            }
        }, EXECUTOR);
    }

    public CompletableFuture<T> queue(Function<Table, T> mapper) {
        return queue(this, mapper);
    }

    public CompletableFuture<Table> queue() {
        return queue(this, Function.identity());
    }

    public CompletableFuture<Void> execute() {
        return execute(EXECUTOR);
    }

    public CompletableFuture<Void> execute(Executor executor) {
        return CompletableFuture.runAsync(() -> {
            Database database = Database.getInstance().connect();
            try {
                database.executeStatement(query);
            } finally {
                database.close();
            }
        }, executor);
    }

    public static CompletableFuture<Void> allOf(DatabaseAction<?>... actions) {
        return CompletableFuture.runAsync(() -> {
            Executor executor = Executors.newSingleThreadExecutor();
            for (DatabaseAction<?> action : actions) {
                action.execute(executor);
            }
        });
    }

}

