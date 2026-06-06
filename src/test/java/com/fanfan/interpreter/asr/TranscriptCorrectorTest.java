package com.fanfan.interpreter.asr;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TranscriptCorrectorTest {
    @Test
    void replacesCommonTechnicalTerms() {
        assertEquals("OpenAI uses WebSocket with DashScope", TranscriptCorrector.correct("open ai uses web socket with dash scope"));
    }

    @Test
    void collapsesDuplicateWords() {
        assertEquals("hello world", TranscriptCorrector.correct("hello hello world"));
    }
}
