package com.fanfan.interpreter.ui;

import org.junit.jupiter.api.Test;

import javax.swing.event.TableModelEvent;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MainWindowTest {
    @Test
    void previewTextKeepsOnlySourceWhenTranslationIsBlank() {
        assertEquals("current source", MainWindow.previewText("current source", ""));
    }

    @Test
    void previewTextKeepsBilingualLinesWhenTranslationExists() {
        assertEquals(
                "current source" + System.lineSeparator() + "当前译文",
                MainWindow.previewText("current source", "当前译文")
        );
    }

    @Test
    void subtitleTableInsertsSingleNewRow() {
        MainWindow.SubtitleTableModel model = new MainWindow.SubtitleTableModel();
        List<TableModelEvent> events = tableEvents(model);

        model.setEntries(List.of(new com.fanfan.interpreter.model.SubtitleEntry("hello", false)));

        assertEquals(TableModelEvent.INSERT, events.getFirst().getType());
        assertEquals(0, events.getFirst().getFirstRow());
    }

    @Test
    void subtitleTableUpdatesLastRowWhenSizeIsUnchanged() {
        MainWindow.SubtitleTableModel model = new MainWindow.SubtitleTableModel();
        model.setEntries(List.of(new com.fanfan.interpreter.model.SubtitleEntry("hello", false)));
        List<TableModelEvent> events = tableEvents(model);

        model.setEntries(List.of(new com.fanfan.interpreter.model.SubtitleEntry("hello world", false)));

        assertEquals(TableModelEvent.UPDATE, events.getFirst().getType());
        assertEquals(0, events.getFirst().getFirstRow());
    }

    @Test
    void bestEntryPrefersTranslatedOverLatestUntranslated() {
        com.fanfan.interpreter.model.SubtitleEntry a = new com.fanfan.interpreter.model.SubtitleEntry("hello", true);
        a.updateTranslation("你好");
        com.fanfan.interpreter.model.SubtitleEntry b = new com.fanfan.interpreter.model.SubtitleEntry("world", true);

        assertEquals(a, MainWindow.bestEntry(List.of(a, b)));
    }

    @Test
    void bestEntryReturnsLatestWhenAllTranslated() {
        com.fanfan.interpreter.model.SubtitleEntry a = new com.fanfan.interpreter.model.SubtitleEntry("hello", true);
        a.updateTranslation("你好");
        com.fanfan.interpreter.model.SubtitleEntry b = new com.fanfan.interpreter.model.SubtitleEntry("world", true);
        b.updateTranslation("世界");

        assertEquals(b, MainWindow.bestEntry(List.of(a, b)));
    }

    @Test
    void bestEntryReturnsLatestWhenNoneTranslated() {
        com.fanfan.interpreter.model.SubtitleEntry a = new com.fanfan.interpreter.model.SubtitleEntry("hello", false);
        com.fanfan.interpreter.model.SubtitleEntry b = new com.fanfan.interpreter.model.SubtitleEntry("world", false);

        assertEquals(b, MainWindow.bestEntry(List.of(a, b)));
    }

    private static List<TableModelEvent> tableEvents(MainWindow.SubtitleTableModel model) {
        List<TableModelEvent> events = new ArrayList<>();
        model.addTableModelListener(events::add);
        return events;
    }
}
