package com.inspien.eai.config;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SchedulerConfig {

    public static void start(Runnable task) {
        Executors.newSingleThreadScheduledExecutor()
                .scheduleAtFixedRate(task, 0, 5, TimeUnit.MINUTES);
    }
}
