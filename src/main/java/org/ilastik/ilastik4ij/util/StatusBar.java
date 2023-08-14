package org.ilastik.ilastik4ij.util;

import org.scijava.app.StatusService;

import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Update status bar while functions are running or collections are iterated.
 */
public final class StatusBar implements AutoCloseable {
    private static final String SPINNER_CHARS = "|/-\\";

    private final ScheduledExecutorService pool;
    private final StatusService status;
    private final int period;

    public StatusBar(StatusService statusService, int updatePeriodMillis) {
        pool = Executors.newScheduledThreadPool(1);
        status = Objects.requireNonNull(statusService);
        period = updatePeriodMillis;
        if (updatePeriodMillis <= 0) {
            throw new IllegalArgumentException("update period should be positive");
        }
    }

    @Override
    public void close() {
        pool.shutdown();
        status.clearStatus();
    }

    /**
     * Periodically update status bar from another thread by showing message with textual spinner.
     */
    public void withSpinner(String message, Runnable func) {
        Objects.requireNonNull(message);
        Objects.requireNonNull(func);

        final int[] index = {0};
        Runnable update = () -> {
            status.showStatus(message + " " + SPINNER_CHARS.charAt(index[0]));
            index[0] = (index[0] + 1) % SPINNER_CHARS.length();
        };

        ScheduledFuture<?> sf = pool.scheduleAtFixedRate(update, 0, period, TimeUnit.MILLISECONDS);
        try {
            func.run();
        } finally {
            sf.cancel(true);
        }
    }

    /**
     * Update status bar each time a new collection item is processed.
     */
    public <E> void withProgress(String message, Collection<E> items, Consumer<E> func) {
        Objects.requireNonNull(message);
        Objects.requireNonNull(items);
        Objects.requireNonNull(func);

        int progress = 0;
        status.showStatus(progress++, items.size(), message);
        for (E item : items) {
            func.accept(item);
            status.showStatus(progress++, items.size(), message);
        }
    }
}
