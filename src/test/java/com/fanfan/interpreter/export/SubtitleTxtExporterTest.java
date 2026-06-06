package com.fanfan.interpreter.export;

import com.fanfan.interpreter.model.SubtitleEntry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SubtitleTxtExporterTest {
    @Test
    void formatsEntriesWithEnglishAndChineseText() {
        SubtitleEntry entry = new SubtitleEntry("hello world", true);
        entry.updateTranslation("你好，世界");

        String text = SubtitleTxtExporter.format(List.of(entry));

        assertTrue(text.contains("EN: hello world"));
        assertTrue(text.contains("ZH: 你好，世界"));
        assertTrue(text.contains(entry.createdAt().toString()));
    }
}
