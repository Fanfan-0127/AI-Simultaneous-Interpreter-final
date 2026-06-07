package com.fanfan.interpreter.translation;

import com.fanfan.interpreter.model.SubtitleEntry;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TranslationSchedulerTest {
    @Test
    void debouncesDraftTranslationsToLatestSourceText() throws Exception {
        RecordingTranslator translator = new RecordingTranslator();
        try (TranslationScheduler scheduler = new TranslationScheduler(translator)) {
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
    void finalTranscriptTriggersImmediateCorrection() throws Exception {
        RecordingTranslator translator = new RecordingTranslator();
        try (TranslationScheduler scheduler = new TranslationScheduler(translator)) {
            SubtitleEntry entry = new SubtitleEntry("final words", true);
            entry.updateTranslation("初稿");
            CountDownLatch latch = new CountDownLatch(1);

            scheduler.translate(entry, true, result -> {
                if (result.translatedText().startsWith("corrected:")) {
                    latch.countDown();
                }
            }, exception -> {
            });

            assertTrue(latch.await(2, TimeUnit.SECONDS));
        }

        assertEquals(List.of("final words|初稿"), translator.corrections());
    }

    @Test
    void skipsStaleQueuedTranslationResult() throws Exception {
        BlockingTranslator translator = new BlockingTranslator();
        try (TranslationScheduler scheduler = new TranslationScheduler(translator)) {
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

    @Test
    void throttlesStreamingTokensButDeliversFinal() throws Exception {
        RapidTokenTranslator translator = new RapidTokenTranslator();
        try (TranslationScheduler scheduler = new TranslationScheduler(translator)) {
            SubtitleEntry entry = new SubtitleEntry("hello world", false);
            List<String> delivered = new CopyOnWriteArrayList<>();
            CountDownLatch done = new CountDownLatch(1);

            scheduler.translate(entry, false, result -> {
                delivered.add(result.translatedText());
                if (result.translatedText().equals("final")) {
                    done.countDown();
                }
            }, exception -> {
            });

            assertTrue(done.await(2, TimeUnit.SECONDS));
            // Should have fewer deliveries than tokens (throttle)
            assertTrue(delivered.size() < translator.totalTokens,
                    "Throttle should reduce deliveries; got " + delivered.size() + " of " + translator.totalTokens);
            // Final token must always be delivered
            assertEquals("final", delivered.getLast());
        }
    }

    private static final class RapidTokenTranslator implements Translator {
        final int totalTokens = 50;

        @Override
        public String translateEnglishToChinese(String englishText) {
            return "final";
        }

        @Override
        public void translateEnglishToChineseStreaming(
                String englishText, Consumer<String> onToken,
                Consumer<String> onComplete, Consumer<Exception> onError) {
            for (int i = 1; i <= totalTokens; i++) {
                onToken.accept("token-" + i);
            }
            onComplete.accept("final");
        }
    }

    private static final class RecordingTranslator implements Translator {
        private final List<String> drafts = new ArrayList<>();
        private final List<String> corrections = new ArrayList<>();

        @Override
        public String translateEnglishToChinese(String englishText) {
            drafts.add(englishText);
            return "draft:" + englishText;
        }

        @Override
        public String refineEnglishToChinese(String englishText, String draftChineseText) {
            corrections.add(englishText + "|" + draftChineseText);
            return "corrected:" + englishText;
        }

        List<String> drafts() {
            return drafts;
        }

        List<String> corrections() {
            return corrections;
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
