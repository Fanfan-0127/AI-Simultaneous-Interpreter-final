package com.fanfan.interpreter.translation;

import com.fanfan.interpreter.model.SubtitleEntry;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TranslationSchedulerTest {
    @Test
    void debouncesDraftTranslationsToLatestSourceText() throws Exception {
        RecordingTranslator translator = new RecordingTranslator();
        try (TranslationScheduler scheduler = new TranslationScheduler(translator, 180)) {
            SubtitleEntry entry = new SubtitleEntry("hello", false);
            scheduler.translate(entry, false, result -> {
            }, exception -> {
            });
            entry.update("hello world", false);

            CountDownLatch latch = new CountDownLatch(1);
            scheduler.translate(entry, false, result -> latch.countDown(), exception -> {
            });

            assertTrue(latch.await(2, TimeUnit.SECONDS));
        }

        assertTrue(translator.drafts().contains("hello world"));
    }

    @Test
    void finalResultDeliversDraftAndCorrectionWithDifferentTexts() throws Exception {
        RecordingTranslator translator = new RecordingTranslator();
        try (TranslationScheduler scheduler = new TranslationScheduler(translator, 180)) {
            SubtitleEntry entry = new SubtitleEntry("hello world", true);
            List<String> results = new CopyOnWriteArrayList<>();
            CountDownLatch latch = new CountDownLatch(2);

            scheduler.translate(entry, true, "hello world", "hello world.", true,
                    result -> { results.add(result.translatedText()); latch.countDown(); },
                    exception -> {
                    });

            assertTrue(latch.await(2, TimeUnit.SECONDS));
            assertEquals(List.of("draft:hello world", "draft:hello world."), results);
        }
    }

    @Test
    void correctionSkippedWhenSameAsDraft() throws Exception {
        RecordingTranslator translator = new RecordingTranslator();
        try (TranslationScheduler scheduler = new TranslationScheduler(translator, 180)) {
            SubtitleEntry entry = new SubtitleEntry("hello world", true);
            List<String> results = new CopyOnWriteArrayList<>();
            CountDownLatch latch = new CountDownLatch(1);

            scheduler.translate(entry, true, "hello world", "hello world", true,
                    result -> { results.add(result.translatedText()); latch.countDown(); },
                    exception -> {
                    });

            assertTrue(latch.await(2, TimeUnit.SECONDS));
            assertEquals(1, results.size());
            assertEquals("draft:hello world", results.getFirst());
        }
    }

    @Test
    void skipsStaleQueuedTranslationResult() throws Exception {
        BlockingTranslator translator = new BlockingTranslator();
        try (TranslationScheduler scheduler = new TranslationScheduler(translator, 180)) {
            SubtitleEntry entry = new SubtitleEntry("old words", false);
            CountDownLatch results = new CountDownLatch(1);
            List<String> translatedResults = new ArrayList<>();

            scheduler.translate(entry, false, result -> {
                translatedResults.add(result.translatedText());
                results.countDown();
            }, exception -> {
            });

            assertTrue(translator.started.await(2, TimeUnit.SECONDS));
            entry.update("new words", false);
            scheduler.translate(entry, false, result -> {
                translatedResults.add(result.translatedText());
                results.countDown();
            }, exception -> {
            });
            translator.release.countDown();

            assertTrue(results.await(2, TimeUnit.SECONDS));
            assertEquals(List.of("draft:new words"), translatedResults);
            assertEquals(2, translator.calls.get());
        }
    }

    private static final class RecordingTranslator implements Translator {
        private final List<String> drafts = new ArrayList<>();

        @Override
        public String translateEnglishToChinese(String englishText) {
            drafts.add(englishText);
            return "draft:" + englishText;
        }

        List<String> drafts() {
            return drafts;
        }
    }

    private static final class BlockingTranslator implements Translator {
        private final CountDownLatch started = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);
        private final AtomicInteger calls = new AtomicInteger();

        @Override
        public String translateEnglishToChinese(String englishText) throws Exception {
            calls.incrementAndGet();
            if (englishText.equals("old words")) {
                started.countDown();
                assertTrue(release.await(2, TimeUnit.SECONDS));
            }
            return "draft:" + englishText;
        }
    }
}
