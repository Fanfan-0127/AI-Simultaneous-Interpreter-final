package com.fanfan.interpreter.model;

import java.time.Instant;
import java.util.UUID;

public record SubtitleRevision(
        String id,
        String subtitleId,
        SubtitleRevisionType type,
        String oldSourceText,
        String newSourceText,
        String oldTranslatedText,
        String newTranslatedText,
        Instant createdAt
) {
    public static SubtitleRevision create(
            String subtitleId,
            SubtitleRevisionType type,
            String oldSourceText,
            String newSourceText,
            String oldTranslatedText,
            String newTranslatedText
    ) {
        return new SubtitleRevision(
                UUID.randomUUID().toString(),
                subtitleId,
                type,
                valueOrEmpty(oldSourceText),
                valueOrEmpty(newSourceText),
                valueOrEmpty(oldTranslatedText),
                valueOrEmpty(newTranslatedText),
                Instant.now()
        );
    }

    public boolean hasVisibleChange() {
        return !oldSourceText.equals(newSourceText) || !oldTranslatedText.equals(newTranslatedText);
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
