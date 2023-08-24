package org.ilastik.ilastik4ij.util;

import org.scijava.app.StatusService;

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Update status bar while functions are running or collections are iterated.
 */
public final class StatusBar implements AutoCloseable {
    private static final String SPINNER_CHARS = "|/-\\";

    public final StatusService service;
    private final ScheduledExecutorService pool;
    private final int period;

    public StatusBar(StatusService statusService, int updatePeriodMillis) {
        service = Objects.requireNonNull(statusService);
        pool = Executors.newScheduledThreadPool(1);
        period = updatePeriodMillis;
        if (updatePeriodMillis <= 0) {
            throw new IllegalArgumentException("update period should be positive");
        }
    }

    @Override
    public void close() {
        pool.shutdown();
        service.clearStatus();
    }

    /**
     * Periodically update status bar from another thread by showing message with textual spinner.
     */
    public void withSpinner(String message, Runnable func) {
        Objects.requireNonNull(message);
        Objects.requireNonNull(func);

        final int[] index = {0};
        Runnable update = () -> {
            service.showStatus(message + " " + SPINNER_CHARS.charAt(index[0]));
            index[0] = (index[0] + 1) % SPINNER_CHARS.length();
        };

        ScheduledFuture<?> sf = pool.scheduleAtFixedRate(update, 0, period, TimeUnit.MILLISECONDS);
        try {
            func.run();
        } finally {
            sf.cancel(true);
        }
    }
}
