package com.fanfan.interpreter.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

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

    public static Map<String, Integer> extractTerms(List<SubtitleEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Integer> frequencies = new HashMap<>();

        for (SubtitleEntry entry : entries) {
            String sourceText = entry.sourceText();
            if (sourceText.isBlank()) {
                continue;
            }

            var matcher = TECHNICAL_PATTERN.matcher(sourceText);
            while (matcher.find()) {
                String term = matcher.group(1).strip();
                if (isValidTerm(term)) {
                    frequencies.merge(term, 1, Integer::sum);
                }
            }
        }

        return frequencies.entrySet().stream()
                .filter(e -> e.getValue() >= MIN_FREQUENCY)
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(50)
                .collect(LinkedHashMap::new,
                        (map, e) -> map.put(e.getKey(), e.getValue()),
                        LinkedHashMap::putAll);
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
}
