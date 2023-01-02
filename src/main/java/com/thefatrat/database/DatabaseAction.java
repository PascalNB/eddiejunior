package com.thefatrat.database;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

@SuppressWarnings("unused")
public class DatabaseAction<T> {

    private static final Executor EXECUTOR = command -> new Thread(command).start();
    private final Query query;

    public DatabaseAction(Query query) {
        this.query = query;
    }

    public DatabaseAction(String query, Object... args) {
        this(Query.of(query, args));
    }

    public CompletableFuture<T> queue(Function<Table, T> mapper, Executor executor) {
        return uncheckedQueue(this, mapper, executor);
    }

    public CompletableFuture<T> queue(Function<Table, T> mapper) {
        return uncheckedQueue(this, mapper, EXECUTOR);
    }

    public CompletableFuture<Table> queue() {
        return uncheckedQueue(this, Function.identity(), EXECUTOR);
    }

    public CompletableFuture<Table> queue(Executor executor) {
        return uncheckedQueue(this, Function.identity(), executor);
    }

    @NotNull
    @Contract("_, _, _ -> new")
    private static <T> CompletableFuture<T> uncheckedQueue(DatabaseAction<?> action, Function<Table, T> mapper,
        Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            AtomicReference<T> reference = new AtomicReference<>();
            Database database = Database.getInstance().connect();
            try {
                database.queryStatement(table -> reference.set(mapper.apply(table)), action.query);
                return reference.get();
            } finally {
                database.close();
            }
        }, executor);
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

    @NotNull
    @Contract("_ -> new")
    public static CompletableFuture<Void> executeAll(DatabaseAction<?>... actions) {
        return executeAll(Executors.newSingleThreadExecutor(), actions);
    }

    @NotNull
    @Contract("_, _ -> new")
    public static CompletableFuture<Void> executeAll(Executor executor, DatabaseAction<?>... actions) {
        return CompletableFuture.runAsync(() -> {
            for (DatabaseAction<?> action : actions) {
                action.execute(executor);
            }
        });
    }

    @NotNull
    public static CompletableFuture<List<Table>> queueAll(DatabaseAction<?>... actions) {
        return uncheckedQueueAll(Function.identity(), EXECUTOR, actions);
    }

    @NotNull
    public static CompletableFuture<List<Table>> queueAll(Executor executor, DatabaseAction<?>... actions) {
        return uncheckedQueueAll(Function.identity(), executor, actions);
    }

    @NotNull
    public static <T> CompletableFuture<List<T>> queueAll(Function<Table, T> mapper, DatabaseAction<?>... actions) {
        return uncheckedQueueAll(mapper, Executors.newSingleThreadExecutor(), actions);
    }

    @NotNull
    @SafeVarargs
    public static <T> CompletableFuture<List<T>> queueAll(Function<Table, T> mapper, Executor executor,
        DatabaseAction<T>... actions) {
        return uncheckedQueueAll(mapper, executor, actions);
    }

    @NotNull
    private static <T> CompletableFuture<List<T>> uncheckedQueueAll(Function<Table, T> mapper, Executor executor,
        @NotNull DatabaseAction<?>... actions) {
        List<T> result = new ArrayList<>();
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (DatabaseAction<?> action : actions) {
            futures.add(action.queue(executor).thenAccept(t -> result.add(mapper.apply(t))));
        }
        return CompletableFuture.supplyAsync(() -> {
            for (CompletableFuture<Void> future : futures) {
                future.join();
            }
            return result;
        });
    }

}

