package com.thefatrat.database.action;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.function.Supplier;

public class MultiDatabaseAction<B, T> implements DatabaseAction<List<T>> {

    private final Collection<? extends DatabaseAction<? extends B>> actions;
    private final Function<B, T> mapper;
    private final Executor executor;
    private final Supplier<ExecutorService> serviceSupplier;

    public MultiDatabaseAction(Collection<? extends DatabaseAction<? extends B>> actions,
        Function<B, T> mapper, Executor executor, Supplier<ExecutorService> serviceSupplier) {
        this.actions = actions;
        this.mapper = mapper;
        this.executor = executor;
        this.serviceSupplier = serviceSupplier;
    }

    @Override
    public CompletableFuture<List<T>> query() {
        return CompletableFuture.supplyAsync(() -> {
            ExecutorService service = serviceSupplier.get();
            List<CompletableFuture<? extends B>> futures = new ArrayList<>();
            for (DatabaseAction<? extends B> action : actions) {
                futures.add(action.withExecutor(service).query());
            }
            List<T> result = new ArrayList<>();
            for (CompletableFuture<? extends B> future : futures) {
                result.add(mapper.apply(future.join()));
            }
            service.shutdown();
            return result;
        }, executor);
    }

    @Override
    public CompletableFuture<Void> execute() {
        return CompletableFuture.runAsync(() -> {
            ExecutorService service = serviceSupplier.get();
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            for (DatabaseAction<?> action : actions) {
                futures.add(action.withExecutor(service).execute());
            }
            for (CompletableFuture<Void> future : futures) {
                future.join();
            }
            service.shutdown();
        }, executor);
    }

    @Override
    public Executor getExecutor() {
        return executor;
    }

    @Override
    public DatabaseAction<List<T>> withExecutor(Executor executor) {
        return new MultiDatabaseAction<>(actions, mapper, executor, serviceSupplier);
    }

}
