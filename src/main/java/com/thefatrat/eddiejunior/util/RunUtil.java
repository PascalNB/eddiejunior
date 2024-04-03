package com.thefatrat.eddiejunior.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class RunUtil {

    private static final ExecutorService EXECUTOR_SERVICE = Executors.newCachedThreadPool();

    public static void run(Runnable runnable) {
        EXECUTOR_SERVICE.execute(runnable);
    }

}
