package com.fanfan.interpreter.asr;

import java.util.Set;
import java.util.regex.Pattern;

public final class SentenceStabilityDetector {
    private static final Set<Character> SENTENCE_ENDERS = Set.of('.', '!', '?', '。', '！', '？');
    private static final Pattern ENDS_WITH_PUNCTUATION =
            Pattern.compile(".*[.!?。！？]$");

    private SentenceStabilityDetector() {}

    public static boolean isStable(String text, boolean finalResult) {
        if (finalResult) {
            return true;
        }
        if (text == null || text.isBlank()) {
            return false;
        }
        return endsWithSentencePunctuation(text);
    }

    static boolean endsWithSentencePunctuation(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String trimmed = text.strip();
        return !trimmed.isEmpty() && SENTENCE_ENDERS.contains(trimmed.charAt(trimmed.length() - 1));
    }
}
