package com.fanfan.interpreter.ui;

import org.junit.jupiter.api.Test;

import javax.swing.JTextArea;
import java.awt.Font;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FloatingSubtitleWindowTest {
    @Test
    void wrappedTranslationHeightGrowsForMultipleLines() {
        JTextArea textArea = new JTextArea();
        textArea.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 28));
        int lineHeight = textArea.getFontMetrics(textArea.getFont()).getHeight();

        int height = FloatingSubtitleWindow.wrappedTextHeight(
                textArea,
                "这是一段很长的翻译字幕内容，需要在透明悬浮窗中自然换行并完整显示下面的每一行文本",
                240,
                4
        );

        assertTrue(height > lineHeight);
    }
}
