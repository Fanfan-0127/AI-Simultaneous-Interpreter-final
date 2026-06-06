package com.fanfan.interpreter.asr;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

public final class StableTranscriptScheduler implements AutoCloseable {
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "asr-stability-window");
        thread.setDaemon(true);
        return thread;
    });
    private final long stabilityDelayMs;
    private ScheduledFuture<?> pending;

    public StableTranscriptScheduler(long stabilityDelayMs) {
        this.stabilityDelayMs = stabilityDelayMs;
    }

    public StableTranscriptScheduler(int stabilityDelayMs) {
        this((long) stabilityDelayMs);
    }

    public synchronized void accept(String text, boolean finalResult, BiConsumer<String, Boolean> stableConsumer) {
        cancelPending();
        if (text == null || text.isBlank()) {
            return;
        }
        if (finalResult) {
            stableConsumer.accept(text, true);
            return;
        }
        pending = scheduler.schedule(
                () -> stableConsumer.accept(text, false),
                stabilityDelayMs,
                TimeUnit.MILLISECONDS
        );
    }

    public synchronized void clear() {
        cancelPending();
    }

    @Override
    public synchronized void close() {
        cancelPending();
        scheduler.shutdownNow();
    }

    private void cancelPending() {
        if (pending != null) {
            pending.cancel(false);
            pending = null;
        }
    }
}
