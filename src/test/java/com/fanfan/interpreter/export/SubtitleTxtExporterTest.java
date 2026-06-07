package com.fanfan.interpreter.export;

import com.fanfan.interpreter.model.SubtitleEntry;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SubtitleTxtExporterTest {
    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

    @Test
    void formatsEntriesWithEnglishAndChineseText() {
        SubtitleEntry entry = new SubtitleEntry("hello world", true);
        entry.updateTranslation("你好，世界");

        String text = SubtitleTxtExporter.format(List.of(entry));

        assertTrue(text.contains("EN: hello world"));
        assertTrue(text.contains("ZH: 你好，世界"));
        assertTrue(text.contains(TIME_FMT.format(entry.createdAt())));
    }
}
