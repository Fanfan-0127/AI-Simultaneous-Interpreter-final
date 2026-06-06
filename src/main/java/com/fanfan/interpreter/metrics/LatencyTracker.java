package com.fanfan.interpreter.metrics;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class LatencyTracker {
    private final Map<String, Instant> timestamps = new ConcurrentHashMap<>();
    private volatile long lastAsrLatencyMs = 0;
    private volatile long lastTranslationLatencyMs = 0;
    private volatile int pendingTranslationTasks = 0;

    public void markAudioCaptured() {
        timestamps.put("audio_captured", Instant.now());
    }

    public void markAsrReceived() {
        Instant captured = timestamps.get("audio_captured");
        if (captured != null) {
            lastAsrLatencyMs = java.time.Duration.between(captured, Instant.now()).toMillis();
        }
        timestamps.put("asr_received", Instant.now());
    }

    public void markTranslationStarted() {
        pendingTranslationTasks++;
        timestamps.put("translation_started", Instant.now());
    }

    public void markTranslationCompleted() {
        Instant started = timestamps.get("translation_started");
        if (started != null) {
            lastTranslationLatencyMs = java.time.Duration.between(started, Instant.now()).toMillis();
        }
        pendingTranslationTasks = Math.max(0, pendingTranslationTasks - 1);
    }

    public long getLastAsrLatencyMs() {
        return lastAsrLatencyMs;
    }

    public long getLastTranslationLatencyMs() {
        return lastTranslationLatencyMs;
    }

    public int getPendingTranslationTasks() {
        return pendingTranslationTasks;
    }

    public String getStatusSummary() {
        return String.format("ASR: %dms | 翻译: %dms | 积压: %d",
                lastAsrLatencyMs, lastTranslationLatencyMs, pendingTranslationTasks);
    }

    public void reset() {
        timestamps.clear();
        lastAsrLatencyMs = 0;
        lastTranslationLatencyMs = 0;
        pendingTranslationTasks = 0;
    }
}
