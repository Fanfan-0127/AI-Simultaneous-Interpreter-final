package com.fanfan.interpreter.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class TermExtractor {
    private static final int MIN_TERM_LENGTH = 3;
    private static final int MAX_TERM_LENGTH = 40;
    private static final int MIN_FREQUENCY = 2;
    private static final Pattern TECHNICAL_PATTERN = Pattern.compile(
            "\\b([A-Z][a-zA-Z0-9+#]*(?:\\s+[A-Z][a-zA-Z0-9+#]*)*)\\b"
    );
    private static final Set<String> STOP_WORDS = Set.of(
            "The", "This", "That", "These", "Those", "What", "Which", "Who",
            "How", "When", "Where", "Why", "Can", "Will", "Would", "Should",
            "About", "With", "From", "Into", "Through", "During", "Before",
            "After", "Above", "Below", "Between", "And", "But", "Or", "Nor",
            "Not", "So", "Yet", "Both", "Either", "Neither", "Each", "Few",
            "More", "Most", "Other", "Some", "Such", "No", "Only", "Own",
            "Same", "Than", "Too", "Very", "Just", "Also", "Now", "Here",
            "There", "Then", "Once", "If", "Because", "As", "Until", "While"
    );

    private TermExtractor() {
    }

    public static Map<String, String> extractTerms(List<SubtitleEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, TermCandidate> candidates = new HashMap<>();

        for (SubtitleEntry entry : entries) {
            String sourceText = entry.sourceText();
            String translatedText = entry.translatedText();

            if (sourceText.isBlank() || translatedText.isBlank()) {
                continue;
            }

            extractCandidates(sourceText, translatedText, candidates);
        }

        return filterAndFormat(candidates);
    }

    private static void extractCandidates(
            String sourceText,
            String translatedText,
            Map<String, TermCandidate> candidates
    ) {
        var matcher = TECHNICAL_PATTERN.matcher(sourceText);
        while (matcher.find()) {
            String term = matcher.group(1).strip();

            if (!isValidTerm(term)) {
                continue;
            }

            candidates.computeIfAbsent(term, k -> new TermCandidate(term))
                    .addTranslation(translatedText);
        }
    }

    private static boolean isValidTerm(String term) {
        if (term.length() < MIN_TERM_LENGTH || term.length() > MAX_TERM_LENGTH) {
            return false;
        }

        if (STOP_WORDS.contains(term)) {
            return false;
        }

        if (term.chars().allMatch(Character::isUpperCase) && term.length() <= 3) {
            return false;
        }

        return true;
    }

    private static Map<String, String> filterAndFormat(Map<String, TermCandidate> candidates) {
        return candidates.values().stream()
                .filter(candidate -> candidate.frequency >= MIN_FREQUENCY)
                .sorted((a, b) -> Integer.compare(b.frequency, a.frequency))
                .limit(50)
                .collect(Collectors.toMap(
                        TermCandidate::getTerm,
                        TermCandidate::getMostCommonTranslation,
                        (existing, replacement) -> existing,
                        LinkedHashMap::new
                ));
    }

    private static final class TermCandidate {
        private final String term;
        private int frequency = 0;
        private final Map<String, Integer> translationCounts = new HashMap<>();

        TermCandidate(String term) {
            this.term = term;
        }

        void addTranslation(String translation) {
            frequency++;
            String normalized = normalizeTranslation(translation);
            if (!normalized.isBlank()) {
                translationCounts.merge(normalized, 1, Integer::sum);
            }
        }

        String getTerm() {
            return term;
        }

        String getMostCommonTranslation() {
            return translationCounts.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("");
        }

        private String normalizeTranslation(String text) {
            String[] parts = text.split("[，,。；;]");
            for (String part : parts) {
                String trimmed = part.strip();
                if (trimmed.length() >= 2 && trimmed.length() <= 30) {
                    return trimmed;
                }
            }
            return text.strip();
        }
    }
}
