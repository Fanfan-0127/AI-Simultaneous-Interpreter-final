package com.fanfan.interpreter.translation;

import com.fanfan.interpreter.model.SubtitleEntry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class TranslationScheduler implements AutoCloseable {
    private static final long DRAFT_DELAY_MS = 180;

    private final Translator translator;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "qwen-mt-translation");
        thread.setDaemon(true);
        return thread;
    });
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "qwen-mt-scheduler");
        thread.setDaemon(true);
        return thread;
    });
    private final Map<String, ScheduledFuture<?>> pendingDrafts = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> pendingCorrections = new ConcurrentHashMap<>();
    private final Map<String, Long> latestVersions = new ConcurrentHashMap<>();
    private final Map<String, String> deliveredDrafts = new ConcurrentHashMap<>();

    public TranslationScheduler(Translator translator) {
        this.translator = translator;
    }

    public void translate(
            SubtitleEntry entry,
            boolean finalResult,
            Consumer<TranslationResult> resultConsumer,
            Consumer<Exception> errorConsumer
    ) {
        String text = entry.sourceText();
        translate(entry, finalResult, text, text, false, resultConsumer, errorConsumer);
    }

    public void translate(
            SubtitleEntry entry,
            boolean finalResult,
            String translationSourceText,
            Consumer<TranslationResult> resultConsumer,
            Consumer<Exception> errorConsumer
    ) {
        translate(entry, finalResult, translationSourceText, translationSourceText, false, resultConsumer, errorConsumer);
    }

    public void translate(
            SubtitleEntry entry,
            boolean finalResult,
            String translationSourceText,
            boolean stableSentence,
            Consumer<TranslationResult> resultConsumer,
            Consumer<Exception> errorConsumer
    ) {
        translate(entry, finalResult, translationSourceText, translationSourceText, stableSentence, resultConsumer, errorConsumer);
    }

    public void translate(
            SubtitleEntry entry,
            boolean finalResult,
            String rawSourceText,
            String correctedSourceText,
            boolean stableSentence,
            Consumer<TranslationResult> resultConsumer,
            Consumer<Exception> errorConsumer
    ) {
        if (entry == null || rawSourceText == null || rawSourceText.isBlank()) {
            return;
        }
        String entryId = entry.id();
        long sourceVersion = entry.sourceVersion();
        String draftText = entry.translatedText();
        String correctionText = correctedSourceText != null ? correctedSourceText : rawSourceText;
        latestVersions.put(entryId, sourceVersion);
        long draftDelay = finalResult ? 0 : DRAFT_DELAY_MS;
        schedule(
                pendingDrafts,
                entryId,
                draftDelay,
                () -> submitDraft(entryId, sourceVersion, rawSourceText, resultConsumer, errorConsumer)
        );
        if (finalResult || stableSentence) {
            schedule(
                    pendingCorrections,
                    entryId,
                    0,
                    () -> submitCorrection(entryId, sourceVersion, correctionText, draftText, resultConsumer, errorConsumer)
            );
        }
    }

    @Override
    public void close() {
        pendingDrafts.values().forEach(future -> future.cancel(false));
        pendingCorrections.values().forEach(future -> future.cancel(false));
        latestVersions.clear();
        deliveredDrafts.clear();
        scheduler.shutdownNow();
        executor.shutdownNow();
    }

    private void schedule(
            Map<String, ScheduledFuture<?>> pendingTasks,
            String entryId,
            long delayMs,
            Runnable task
    ) {
        ScheduledFuture<?> previous = pendingTasks.remove(entryId);
        if (previous != null) {
            previous.cancel(false);
        }
        ScheduledFuture<?> next = scheduler.schedule(() -> {
            pendingTasks.remove(entryId);
            task.run();
        }, delayMs, TimeUnit.MILLISECONDS);
        pendingTasks.put(entryId, next);
    }

    private void submitDraft(
            String entryId,
            long sourceVersion,
            String sourceText,
            Consumer<TranslationResult> resultConsumer,
            Consumer<Exception> errorConsumer
    ) {
        executor.submit(() -> {
            try {
                if (stale(entryId, sourceVersion)) {
                    return;
                }
                String translatedText = translator.translateEnglishToChinese(sourceText);
                if (stale(entryId, sourceVersion)) {
                    return;
                }
                deliveredDrafts.put(entryId, translatedText);
                resultConsumer.accept(new TranslationResult(entryId, sourceVersion, translatedText));
            } catch (Exception exception) {
                errorConsumer.accept(exception);
            }
        });
    }

    private void submitCorrection(
            String entryId,
            long sourceVersion,
            String sourceText,
            String draftText,
            Consumer<TranslationResult> resultConsumer,
            Consumer<Exception> errorConsumer
    ) {
        executor.submit(() -> {
            try {
                if (stale(entryId, sourceVersion)) {
                    return;
                }
                String translatedText = translator.translateEnglishToChinese(sourceText);
                if (stale(entryId, sourceVersion)) {
                    return;
                }
                String previous = deliveredDrafts.get(entryId);
                if (translatedText.equals(previous)) {
                    return;
                }
                deliveredDrafts.put(entryId, translatedText);
                resultConsumer.accept(new TranslationResult(entryId, sourceVersion, translatedText));
            } catch (Exception exception) {
                errorConsumer.accept(exception);
            }
        });
    }

    private boolean stale(String entryId, long sourceVersion) {
        return latestVersions.getOrDefault(entryId, sourceVersion) != sourceVersion;
    }

    public record TranslationResult(String entryId, long sourceVersion, String translatedText) {
    }
}
