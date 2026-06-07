package com.fanfan.interpreter.asr;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SentenceStabilityDetectorTest {

    @Test
    void finalResultAlwaysStable() {
        assertTrue(SentenceStabilityDetector.isStable("hello", true));
        assertTrue(SentenceStabilityDetector.isStable("hello world", true));
        assertTrue(SentenceStabilityDetector.isStable("", true));
    }

    @Test
    void nonFinalWithEnglishPunctuationIsStable() {
        assertTrue(SentenceStabilityDetector.isStable("hello world.", false));
        assertTrue(SentenceStabilityDetector.isStable("how are you?", false));
        assertTrue(SentenceStabilityDetector.isStable("great!", false));
    }

    @Test
    void nonFinalWithChinesePunctuationIsStable() {
        assertTrue(SentenceStabilityDetector.isStable("你好世界。", false));
        assertTrue(SentenceStabilityDetector.isStable("你好吗？", false));
        assertTrue(SentenceStabilityDetector.isStable("太好了！", false));
    }

    @Test
    void nonFinalWithoutPunctuationIsNotStable() {
        assertFalse(SentenceStabilityDetector.isStable("hello world", false));
        assertFalse(SentenceStabilityDetector.isStable("partial sent", false));
        assertFalse(SentenceStabilityDetector.isStable("你好世界", false));
    }

    @Test
    void blankTextIsNotStable() {
        assertFalse(SentenceStabilityDetector.isStable("", false));
        assertFalse(SentenceStabilityDetector.isStable("   ", false));
        assertFalse(SentenceStabilityDetector.isStable(null, false));
    }

    @Test
    void commaIsNotSentenceEnder() {
        assertFalse(SentenceStabilityDetector.isStable("hello,", false));
        assertFalse(SentenceStabilityDetector.isStable("你好，", false));
    }
}
