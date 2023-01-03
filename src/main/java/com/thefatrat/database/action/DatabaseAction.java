package com.thefatrat.database.action;

import com.thefatrat.database.Database;
import com.thefatrat.database.DatabaseAuthenticator;
import com.thefatrat.database.Query;
import com.thefatrat.database.Table;
import org.jetbrains.annotations.Contract;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Function;

@SuppressWarnings("unused")
public interface DatabaseAction<T> {

    Executor DEFAULT_EXECUTOR = r -> new Thread(r).start();

    static <T> DatabaseAction<T> of(Query query, Function<Table, T> mapper) {
        return new SingleDatabaseAction<>(query, mapper, DEFAULT_EXECUTOR);
    }

    static DatabaseAction<Table> of(Query query) {
        return of(query, Function.identity());
    }

    static DatabaseAction<Table> of(String query, Object... args) {
        return of(Query.of(query, args), Function.identity());
    }

    static <T> DatabaseAction<List<T>> allOf(Collection<? extends DatabaseAction<Table>> actions,
        Function<Table, T> mapper) {
        return new MultiDatabaseAction<>(actions, mapper, DEFAULT_EXECUTOR, Executors::newSingleThreadExecutor);
    }

    static <T> DatabaseAction<List<T>> allOf(Collection<? extends DatabaseAction<? extends T>> actions) {
        return new MultiDatabaseAction<>(actions, Function.identity(), DEFAULT_EXECUTOR,
            Executors::newSingleThreadExecutor);
    }

    @SafeVarargs
    static <T> DatabaseAction<List<T>> allOf(Function<Table, T> mapper, DatabaseAction<Table>... actions) {
        return allOf(List.of(actions), mapper);
    }

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

    static void main(String[] args) {
        DatabaseAuthenticator.getInstance().authenticate();
        of("SELECT * FROM component WHERE enabled=?", 1)
            .query()
            .thenAccept(Database::printQueryResult);
    }

}
