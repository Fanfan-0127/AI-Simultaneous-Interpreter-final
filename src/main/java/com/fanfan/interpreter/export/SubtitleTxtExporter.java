package com.fanfan.interpreter.export;

import com.fanfan.interpreter.model.SubtitleEntry;
import com.fanfan.interpreter.model.SubtitleStatus;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class SubtitleTxtExporter {
    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());
    private static final String SEPARATOR = "─".repeat(48);

    private SubtitleTxtExporter() {}

    public static String format(List<SubtitleEntry> entries) {
        StringBuilder builder = new StringBuilder();
        if (!entries.isEmpty()) {
            SubtitleEntry first = entries.getFirst();
            builder.append("# Source: ").append(first.sourceLanguage())
                    .append(" → Target: ").append(first.targetLanguage())
                    .append(System.lineSeparator());
            builder.append(SEPARATOR).append(System.lineSeparator());
        }
        int index = 0;
        for (SubtitleEntry entry : entries) {
            if (entry.sourceText().isBlank() && entry.translatedText().isBlank()) continue;
            index++;
            String time = TIME_FMT.format(entry.createdAt());
            String status = statusLabel(entry);
            builder.append("#").append(index)
                    .append("  [").append(time).append("]  ").append(status)
                    .append(System.lineSeparator());
            builder.append("EN: ").append(entry.sourceText()).append(System.lineSeparator());
            builder.append("ZH: ").append(entry.translatedText()).append(System.lineSeparator());
            builder.append(SEPARATOR).append(System.lineSeparator());
        }
        return builder.toString();
    }

    private static String statusLabel(SubtitleEntry entry) {
        if (entry.status() == SubtitleStatus.CORRECTED) return "CORRECTED";
        if (entry.finalResult()) return "FINAL";
        return "DRAFT";
    }

    public static void export(List<SubtitleEntry> entries, Path outputPath) throws IOException {
        Files.writeString(outputPath, format(entries), StandardCharsets.UTF_8);
    }
}
