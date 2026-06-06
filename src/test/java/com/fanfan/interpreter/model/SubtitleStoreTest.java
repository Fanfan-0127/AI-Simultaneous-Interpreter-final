package com.fanfan.interpreter.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubtitleStoreTest {
    @Test
    void ignoresTranslationForOutdatedTranscriptVersion() {
        SubtitleStore store = new SubtitleStore();
        SubtitleStore.SubtitleUpdate first = store.applyTranscript("hello", false);
        String entryId = first.entry().id();
        long oldVersion = first.entry().sourceVersion();

        store.applyTranscript("hello world", false);
        store.applyTranslation(entryId, oldVersion, "你好");

        SubtitleEntry entry = store.snapshot().getFirst();
        assertEquals("hello world", entry.sourceText());
        assertEquals("", entry.translatedText());
    }

    @Test
    void appliesTranslationForCurrentTranscriptVersion() {
        SubtitleStore store = new SubtitleStore();
        SubtitleStore.SubtitleUpdate update = store.applyTranscript("hello", false);

        store.applyTranslation(update.entry().id(), update.entry().sourceVersion(), "你好");

        assertEquals("你好", store.snapshot().getFirst().translatedText());
    }

    @Test
    void hotUpdatesTranscriptWithoutRecordingAsrRevision() {
        SubtitleStore store = new SubtitleStore();
        store.applyTranscript("hello", false);

        store.applyTranscript("hello world", false);

        assertEquals("hello world", store.snapshot().getFirst().sourceText());
        assertTrue(store.revisionSnapshot().isEmpty());
    }

    @Test
    void recordsDraftAndCorrectionTranslationRevisions() {
        SubtitleStore store = new SubtitleStore();
        SubtitleStore.SubtitleUpdate update = store.applyTranscript("hello", false);

        store.applyTranslation(update.entry().id(), update.entry().sourceVersion(), "你好");
        store.applyTranslation(update.entry().id(), update.entry().sourceVersion(), "你好呀");

        assertEquals(2, store.revisionSnapshot().size());
        assertEquals(SubtitleRevisionType.MT_DRAFT, store.revisionSnapshot().getFirst().type());
        assertEquals(SubtitleRevisionType.MT_CORRECTION, store.revisionSnapshot().getLast().type());
        assertEquals(SubtitleStatus.CORRECTED, store.snapshot().getFirst().status());
    }

    @Test
    void startsNewDraftWhenAccumulatedTranscriptGetsTooLong() {
        SubtitleStore store = new SubtitleStore();
        String firstPart = "word ".repeat(45).strip();
        store.applyTranscript(firstPart, false);

        store.applyTranscript(firstPart + " next words", false);

        assertEquals(2, store.snapshot().size());
        assertEquals("next words", store.snapshot().getLast().sourceText());
    }

    @Test
    void keepsTailWhenLongDraftDoesNotSharePreviousPrefix() {
        SubtitleStore store = new SubtitleStore();
        store.applyTranscript("first draft", false);

        store.applyTranscript("word ".repeat(60).strip(), false);

        assertEquals(2, store.snapshot().size());
        assertTrue(store.snapshot().getLast().sourceText().length() <= 220);
    }

    @Test
    void clearRemovesEntriesAndRevisions() {
        SubtitleStore store = new SubtitleStore();
        SubtitleStore.SubtitleUpdate update = store.applyTranscript("hello", false);
        store.applyTranslation(update.entry().id(), update.entry().sourceVersion(), "你好");

        store.clear();

        assertTrue(store.snapshot().isEmpty());
        assertTrue(store.revisionSnapshot().isEmpty());
    }
}
