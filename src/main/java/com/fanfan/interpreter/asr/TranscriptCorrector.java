package com.fanfan.interpreter.asr;

import java.util.List;
import java.util.regex.Pattern;

public final class TranscriptCorrector {
    private static final List<Replacement> REPLACEMENTS = List.of(
            new Replacement("\\bopen ai\\b", "OpenAI"),
            new Replacement("\\bchat gpt\\b", "ChatGPT"),
            new Replacement("\\bq and a\\b", "Q&A"),
            new Replacement("\\bkubernetees\\b", "Kubernetes"),
            new Replacement("\\bkubernetes\\b", "Kubernetes"),
            new Replacement("\\bdash scope\\b", "DashScope"),
            new Replacement("\\bweb socket\\b", "WebSocket"),
            new Replacement("\\bjava script\\b", "JavaScript"),
            new Replacement("\\btype script\\b", "TypeScript")
    );
    private static final Pattern DUPLICATE_WORDS = Pattern.compile("\\b(?i)([a-z][a-z0-9+.#-]*)\\s+\\1\\b");

    private TranscriptCorrector() {
    }

    public static String correct(String transcript) {
        if (transcript == null || transcript.isBlank()) {
            return "";
        }
        String corrected = transcript.strip().replaceAll("\\s+", " ");
        corrected = DUPLICATE_WORDS.matcher(corrected).replaceAll("$1");
        for (Replacement replacement : REPLACEMENTS) {
            corrected = replacement.apply(corrected);
        }
        return corrected;
    }

    private record Replacement(Pattern pattern, String value) {
        Replacement(String regex, String value) {
            this(Pattern.compile(regex, Pattern.CASE_INSENSITIVE), value);
        }

        String apply(String text) {
            return pattern.matcher(text).replaceAll(value);
        }
    }
}
