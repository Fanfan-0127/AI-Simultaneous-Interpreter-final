package com.fanfan.interpreter.translation;

import com.fanfan.interpreter.model.SubtitleEntry;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Consumer;

public final class TranslationScheduler implements AutoCloseable {
    private static final long DRAFT_DELAY_MS = 180;
    private static final long CORRECTION_DELAY_MS = 650;
    private final Translator translator;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "qwen-mt-translation"); thread.setDaemon(true); return thread;
    });
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "qwen-mt-scheduler"); thread.setDaemon(true); return thread;
    });
    private final Map<String, ScheduledFuture<?>> pendingDrafts = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> pendingCorrections = new ConcurrentHashMap<>();

    public TranslationScheduler(Translator translator) { this.translator = translator; }

    public void translate(SubtitleEntry entry, boolean finalResult,
                          Consumer<TranslationResult> resultConsumer, Consumer<Exception> errorConsumer) {
        if (entry == null || entry.sourceText().isBlank()) return;
        String entryId = entry.id();
        long sourceVersion = entry.sourceVersion();
        String sourceText = entry.sourceText();
        String draftText = entry.translatedText();
        schedule(pendingDrafts, entryId, DRAFT_DELAY_MS,
                () -> submitDraft(entryId, sourceVersion, sourceText, resultConsumer, errorConsumer));
        schedule(pendingCorrections, entryId, finalResult ? 0 : CORRECTION_DELAY_MS,
                () -> submitCorrection(entryId, sourceVersion, sourceText, draftText, resultConsumer, errorConsumer));
    }

    @Override public void close() {
        pendingDrafts.values().forEach(f -> f.cancel(false));
        pendingCorrections.values().forEach(f -> f.cancel(false));
        scheduler.shutdownNow();
        executor.shutdownNow();
    }

    private void schedule(Map<String, ScheduledFuture<?>> pending, String entryId, long delayMs, Runnable task) {
        ScheduledFuture<?> prev = pending.remove(entryId);
        if (prev != null) prev.cancel(false);
        ScheduledFuture<?> next = scheduler.schedule(() -> { pending.remove(entryId); task.run(); },
                delayMs, TimeUnit.MILLISECONDS);
        pending.put(entryId, next);
    }

    private void submitDraft(String entryId, long sourceVersion, String sourceText,
                              Consumer<TranslationResult> resultConsumer, Consumer<Exception> errorConsumer) {
        executor.submit(() -> {
            try {
                String translated = translator.translateEnglishToChinese(sourceText);
                resultConsumer.accept(new TranslationResult(entryId, sourceVersion, translated));
            } catch (Exception e) { errorConsumer.accept(e); }
        });
    }

    private void submitCorrection(String entryId, long sourceVersion, String sourceText, String draftText,
                                   Consumer<TranslationResult> resultConsumer, Consumer<Exception> errorConsumer) {
        executor.submit(() -> {
            try {
                String translated = translator.refineEnglishToChinese(sourceText, draftText);
                resultConsumer.accept(new TranslationResult(entryId, sourceVersion, translated));
            } catch (Exception e) { errorConsumer.accept(e); }
        });
    }

    public record TranslationResult(String entryId, long sourceVersion, String translatedText) {}
}
