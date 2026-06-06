package com.fanfan.interpreter.model;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class SubtitleStore {
    private final List<SubtitleEntry> entries = new CopyOnWriteArrayList<>();
    private final List<SubtitleRevision> revisions = new CopyOnWriteArrayList<>();
    private volatile SubtitleEntry activeEntry;

    public synchronized SubtitleUpdate applyTranscript(String text, boolean finalResult) {
        if (text == null || text.isBlank()) {
            return new SubtitleUpdate(null, snapshot(), revisionSnapshot());
        }
        String normalizedText = text.strip();
        if (activeEntry == null || activeEntry.finalResult()) {
            activeEntry = new SubtitleEntry(normalizedText, finalResult);
            entries.add(activeEntry);
            SubtitleEntry updatedEntry = activeEntry;
            if (finalResult) activeEntry = null;
            return new SubtitleUpdate(updatedEntry, snapshot(), revisionSnapshot());
        }
        activeEntry.update(normalizedText, finalResult);
        SubtitleEntry updatedEntry = activeEntry;
        if (finalResult) activeEntry = null;
        return new SubtitleUpdate(updatedEntry, snapshot(), revisionSnapshot());
    }

    public synchronized List<SubtitleEntry> applyTranslation(String entryId, long sourceVersion, String translatedText) {
        for (SubtitleEntry entry : entries) {
            if (entry.id().equals(entryId) && entry.sourceVersion() == sourceVersion) {
                SubtitleRevision revision = entry.updateTranslation(translatedText);
                addRevisionIfChanged(revision);
                break;
            }
        }
        return snapshot();
    }

    public List<SubtitleEntry> snapshot() { return new ArrayList<>(entries); }
    public List<SubtitleRevision> revisionSnapshot() { return new ArrayList<>(revisions); }

    public synchronized void clear() {
        entries.clear();
        revisions.clear();
        activeEntry = null;
    }

    private void addRevisionIfChanged(SubtitleRevision revision) {
        if (revision.hasVisibleChange()) revisions.add(revision);
    }

    public record SubtitleUpdate(SubtitleEntry entry, List<SubtitleEntry> entries, List<SubtitleRevision> revisions) {}
}
