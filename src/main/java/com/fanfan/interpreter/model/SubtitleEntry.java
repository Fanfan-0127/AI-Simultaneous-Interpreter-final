package com.fanfan.interpreter.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class SubtitleEntry {
    private final String id;
    private final Instant createdAt;
    private String sourceText;
    private String translatedText;
    private boolean finalResult;
    private long sourceVersion;
    private SubtitleStatus status;
    private final List<SubtitleRevision> revisions;
    private final String sourceLanguage;
    private final String targetLanguage;

    public SubtitleEntry(String sourceText, boolean finalResult) {
        this(sourceText, finalResult, "en", "Chinese");
    }

    public SubtitleEntry(String sourceText, boolean finalResult, String sourceLanguage, String targetLanguage) {
        this.id = UUID.randomUUID().toString();
        this.createdAt = Instant.now();
        this.sourceText = sourceText;
        this.translatedText = "";
        this.finalResult = finalResult;
        this.status = finalResult ? SubtitleStatus.FINAL : SubtitleStatus.DRAFT;
        this.revisions = new ArrayList<>();
        this.sourceLanguage = sourceLanguage != null ? sourceLanguage : "en";
        this.targetLanguage = targetLanguage != null ? targetLanguage : "Chinese";
    }

    public String id() { return id; }
    public Instant createdAt() { return createdAt; }
    public String sourceText() { return sourceText; }
    public String translatedText() { return translatedText; }
    public boolean finalResult() { return finalResult; }
    public long sourceVersion() { return sourceVersion; }
    public SubtitleStatus status() { return status; }
    public List<SubtitleRevision> revisions() { return List.copyOf(revisions); }
    public String sourceLanguage() { return sourceLanguage; }
    public String targetLanguage() { return targetLanguage; }

    public SubtitleRevision update(String sourceText, boolean finalResult) {
        String oldSourceText = this.sourceText;
        this.sourceText = sourceText;
        this.finalResult = finalResult;
        this.status = finalResult ? SubtitleStatus.FINAL : SubtitleStatus.DRAFT;
        this.sourceVersion++;
        SubtitleRevision revision = SubtitleRevision.create(
                id, SubtitleRevisionType.ASR_UPDATE,
                oldSourceText, sourceText, translatedText, translatedText);
        addRevisionIfChanged(revision);
        return revision;
    }

    public SubtitleRevision updateTranslation(String translatedText) {
        String oldTranslatedText = this.translatedText;
        this.translatedText = translatedText == null ? "" : translatedText.strip();
        SubtitleRevisionType type = oldTranslatedText.isBlank()
                ? SubtitleRevisionType.MT_DRAFT : SubtitleRevisionType.MT_CORRECTION;
        if (type == SubtitleRevisionType.MT_CORRECTION) {
            this.status = SubtitleStatus.CORRECTED;
        }
        SubtitleRevision revision = SubtitleRevision.create(
                id, type, sourceText, sourceText, oldTranslatedText, this.translatedText);
        addRevisionIfChanged(revision);
        return revision;
    }

    private void addRevisionIfChanged(SubtitleRevision revision) {
        if (revision.hasVisibleChange()) {
            revisions.add(revision);
        }
    }
}
