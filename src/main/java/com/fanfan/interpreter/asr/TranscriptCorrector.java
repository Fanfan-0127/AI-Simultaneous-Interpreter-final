package com.fanfan.interpreter.asr;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public final class TranscriptCorrector {
    private static final Set<String> TERM_WHITELIST = ConcurrentHashMap.newKeySet();
    private static final Set<String> CORE_TERMS = Set.of(
            "api", "sdk", "http", "https", "json", "xml", "css", "html",
            "sql", "nosql", "jwt", "oauth", "rest", "graphql"
    );

    private enum Confidence { HIGH, MEDIUM }

    private record Replacement(Pattern pattern, String value, Confidence confidence) {
        Replacement(String regex, String value) {
            this(regex, value, Confidence.HIGH);
        }

        Replacement(String regex, String value, Confidence confidence) {
            this(Pattern.compile(regex, Pattern.CASE_INSENSITIVE), value, confidence);
        }

        String apply(String text) {
            if (confidence == Confidence.HIGH) {
                return pattern.matcher(text).replaceAll(value);
            }
            return applyWithWhitelist(text);
        }

        private String applyWithWhitelist(String text) {
            var matcher = pattern.matcher(text);
            StringBuilder result = new StringBuilder();
            while (matcher.find()) {
                String matched = matcher.group();
                if (isWhitelisted(matched)) {
                    matcher.appendReplacement(result, java.util.regex.Matcher.quoteReplacement(matched));
                } else {
                    matcher.appendReplacement(result, value);
                }
            }
            matcher.appendTail(result);
            return result.toString();
        }

        private static boolean isWhitelisted(String matched) {
            String lower = matched.toLowerCase(Locale.ROOT);
            return TERM_WHITELIST.contains(lower) || CORE_TERMS.contains(lower);
        }
    }

    private static final List<Replacement> REPLACEMENTS = List.of(
            new Replacement("\\bopen ai\\b", "OpenAI"),
            new Replacement("\\bchat gpt\\b", "ChatGPT"),
            new Replacement("\\bq and a\\b", "Q&A"),
            new Replacement("\\bkubernetees\\b", "Kubernetes"),
            new Replacement("\\bkubernetes\\b", "Kubernetes"),
            new Replacement("\\bdash scope\\b", "DashScope"),
            new Replacement("\\bweb socket\\b", "WebSocket"),
            new Replacement("\\bjava script\\b", "JavaScript"),
            new Replacement("\\btype script\\b", "TypeScript"),
            new Replacement("\\b(?i)ai\\b", "AI", Confidence.MEDIUM),
            new Replacement("\\b(?i)ml\\b", "ML", Confidence.MEDIUM)
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

    public static void addTerms(Collection<String> terms) {
        if (terms == null || terms.isEmpty()) {
            return;
        }
        for (String term : terms) {
            if (term != null && !term.isBlank()) {
                TERM_WHITELIST.add(term.toLowerCase(Locale.ROOT).strip());
            }
        }
    }

    public static int whitelistSize() {
        return TERM_WHITELIST.size();
    }

    public static void clearWhitelist() {
        TERM_WHITELIST.clear();
    }
}
