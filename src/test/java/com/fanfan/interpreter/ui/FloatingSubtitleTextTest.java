package com.fanfan.interpreter.ui;

import org.junit.jupiter.api.Test;

import javax.swing.JTextArea;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FloatingSubtitleTextTest {
    @Test
    void returnsLatestChineseSentenceAfterPunctuation() {
        assertEquals("现在展示最新一句", FloatingSubtitleText.latestSentence("上一句已经结束。现在展示最新一句"));
    }

    @Test
    void returnsLatestEnglishSentenceAfterPunctuation() {
        assertEquals("show only the current sentence", FloatingSubtitleText.latestSentence("The previous sentence is done. show only the current sentence"));
    }

    @Test
    void keepsSentenceWhenNoDelimiterExists() {
        assertEquals("partial subtitle without punctuation", FloatingSubtitleText.latestSentence("partial subtitle without punctuation"));
    }

    @Test
    void keepsCompletedTrailingSentenceWhenTextEndsWithDelimiter() {
        assertEquals("第二句。", FloatingSubtitleText.latestSentence("第一句。第二句。"));
    }

    @Test
    void compactKeepsLatestSentenceWithinBudget() {
        assertEquals("show the compact sentence", FloatingSubtitleText.compact(
                "The previous sentence is done. show the compact sentence",
                80
        ));
    }

    @Test
    void compactKeepsTailWhenLatestSentenceIsTooLong() {
        assertEquals("...terms that matter most", FloatingSubtitleText.compact(
                "This is a very long subtitle line with many words and technical terms that matter most",
                29
        ));
    }

    @Test
    void compactNormalizesWhitespace() {
        assertEquals("hello world", FloatingSubtitleText.compact("hello   \n  world", 40));
    }

    @Test
    void countsWrappedLinesForLongText() {
        JTextArea textArea = new JTextArea();
        textArea.setFont(new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.BOLD, 28));

        int lines = FloatingSubtitleText.wrappedLineCount(
                "这是一段很长的翻译字幕内容，需要在透明悬浮窗中自然换行并完整显示下面的每一行文本",
                textArea.getFontMetrics(textArea.getFont()),
                240
        );

        assertTrue(lines > 1);
    }
}
