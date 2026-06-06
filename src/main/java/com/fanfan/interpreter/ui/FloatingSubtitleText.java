package com.fanfan.interpreter.ui;

import java.awt.FontMetrics;

public final class FloatingSubtitleText {
    private static final String SENTENCE_DELIMITERS = ".!?。！？；;：:";
    private static final String ELLIPSIS = "...";

    private FloatingSubtitleText() {
    }

    public static String latestSentence(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String normalized = text.strip();
        int lastDelimiter = lastDelimiterBeforeTrailingSentence(normalized);
        if (lastDelimiter < 0 || lastDelimiter >= normalized.length() - 1) {
            return normalized;
        }
        return normalized.substring(lastDelimiter + 1).strip();
    }

    public static String compact(String text, int maxCharacters) {
        if (text == null || text.isBlank() || maxCharacters <= 0) {
            return "";
        }
        String latest = latestSentence(text).replaceAll("\\s+", " ").strip();
        if (latest.length() <= maxCharacters) {
            return latest;
        }
        if (maxCharacters <= ELLIPSIS.length()) {
            return latest.substring(latest.length() - maxCharacters);
        }
        String tail = latest.substring(latest.length() - maxCharacters + ELLIPSIS.length()).stripLeading();
        int firstSpace = tail.indexOf(' ');
        if (firstSpace > 0 && firstSpace < tail.length() - 1) {
            tail = tail.substring(firstSpace + 1);
        }
        return ELLIPSIS + tail;
    }

    static int wrappedLineCount(String text, FontMetrics metrics, int width) {
        if (text == null || text.isBlank()) {
            return 1;
        }
        int availableWidth = Math.max(1, width);
        int lines = 0;
        for (String paragraph : text.split("\\R", -1)) {
            lines += wrappedParagraphLineCount(paragraph, metrics, availableWidth);
        }
        return Math.max(1, lines);
    }

    private static int wrappedParagraphLineCount(String paragraph, FontMetrics metrics, int width) {
        if (paragraph == null || paragraph.isBlank()) {
            return 1;
        }
        int lines = 1;
        int lineWidth = 0;
        for (String token : paragraph.strip().split("(?<=\\s)|(?=\\s)")) {
            int tokenWidth = metrics.stringWidth(token);
            if (lineWidth > 0 && lineWidth + tokenWidth > width) {
                lines++;
                lineWidth = 0;
            }
            if (tokenWidth > width) {
                lines += Math.max(0, (int) Math.ceil((double) tokenWidth / width) - 1);
                lineWidth = tokenWidth % width;
                continue;
            }
            lineWidth += tokenWidth;
        }
        return lines;
    }

    private static int lastDelimiterBeforeTrailingSentence(String text) {
        int end = text.length() - 1;
        while (end >= 0 && isDelimiter(text.charAt(end))) {
            end--;
        }
        for (int index = end; index >= 0; index--) {
            if (isDelimiter(text.charAt(index))) {
                return index;
            }
        }
        return -1;
    }

    private static boolean isDelimiter(char character) {
        return SENTENCE_DELIMITERS.indexOf(character) >= 0;
    }
}
