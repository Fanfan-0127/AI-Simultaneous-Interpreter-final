package com.fanfan.interpreter.export;

import com.fanfan.interpreter.model.SubtitleEntry;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class SubtitleTxtExporter {
    private SubtitleTxtExporter() {}

    public static String format(List<SubtitleEntry> entries) {
        StringBuilder builder = new StringBuilder();
        for (SubtitleEntry entry : entries) {
            if (entry.sourceText().isBlank() && entry.translatedText().isBlank()) continue;
            builder.append("[").append(entry.createdAt()).append("]").append(System.lineSeparator());
            builder.append("EN: ").append(entry.sourceText()).append(System.lineSeparator());
            builder.append("ZH: ").append(entry.translatedText()).append(System.lineSeparator()).append(System.lineSeparator());
        }
        return builder.toString();
    }

    public static void export(List<SubtitleEntry> entries, Path outputPath) throws IOException {
        Files.writeString(outputPath, format(entries), StandardCharsets.UTF_8);
    }
}
