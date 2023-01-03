package com.thefatrat.database.action;

import com.thefatrat.database.Query;
import com.thefatrat.database.Table;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Function;

@SuppressWarnings("unused")
public interface DatabaseAction<T> {

    Executor DEFAULT_EXECUTOR = r -> new Thread(r).start();

    @NotNull
    @Contract(value = "_, _ -> new", pure = true)
    static <T> DatabaseAction<T> of(Query query, Function<Table, T> mapper) {
        return new SingleDatabaseAction<>(query, mapper, DEFAULT_EXECUTOR);
    }

    @NotNull
    @Contract(value = "_ -> new", pure = true)
    static DatabaseAction<Table> of(Query query) {
        return of(query, Function.identity());
    }

    @NotNull
    @Contract(value = "_, _ -> new", pure = true)
    static DatabaseAction<Table> of(String query, Object... args) {
        return of(Query.of(query, args), Function.identity());
    }

    @NotNull
    @Contract(value = "_, _ -> new", pure = true)
    static <U, T> DatabaseAction<List<T>> allOf(Collection<? extends DatabaseAction<U>> actions,
        Function<U, T> mapper) {
        return new MultiDatabaseAction<>(actions, mapper, DEFAULT_EXECUTOR, Executors::newSingleThreadExecutor);
    }

    @NotNull
    @Contract(value = "_ -> new", pure = true)
    static <T> DatabaseAction<List<T>> allOf(Collection<? extends DatabaseAction<? extends T>> actions) {
        return new MultiDatabaseAction<>(actions, Function.identity(), DEFAULT_EXECUTOR,
            Executors::newSingleThreadExecutor);
    }

    @NotNull
    @Contract(value = "_, _ -> new", pure = true)
    @SafeVarargs
    static <U, T> DatabaseAction<List<T>> allOf(Function<U, T> mapper, DatabaseAction<U>... actions) {
        return allOf(List.of(actions), mapper);
    }

    @NotNull
    @Contract(value = "_, _ -> new", pure = true)
    static <U, T> DatabaseAction<List<T>> allOf(Function<U, T> mapper, Collection<DatabaseAction<U>> actions) {
        return allOf(actions, mapper);
    }

    @NotNull
    @Contract(value = "_ -> new", pure = true)
    @SafeVarargs
    static <T> DatabaseAction<List<T>> allOf(DatabaseAction<? extends T>... actions) {
        return allOf(List.of(actions));
    }

    CompletableFuture<T> query();

    default <U> CompletableFuture<U> query(Function<T, U> map) {
        return query().thenApply(map);
    }

    CompletableFuture<Void> execute();

    Executor getExecutor();

    @Contract("_ -> new")
    DatabaseAction<T> withExecutor(Executor executor);

}
