package com.fanfan.interpreter.asr;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TranscriptCorrectorTest {

    @AfterEach
    void tearDown() {
        TranscriptCorrector.clearWhitelist();
    }

    @Test
    void replacesCommonTechnicalTerms() {
        assertEquals("OpenAI uses WebSocket with DashScope",
                TranscriptCorrector.correct("open ai uses web socket with dash scope"));
    }

    @Test
    void collapsesDuplicateWords() {
        assertEquals("hello world", TranscriptCorrector.correct("hello hello world"));
    }

    @Test
    void highConfidenceAlwaysReplaces() {
        assertEquals("JavaScript and TypeScript",
                TranscriptCorrector.correct("java script and type script"));
    }

    @Test
    void mediumConfidenceRespectsWhitelist() {
        TranscriptCorrector.addTerms(List.of("ml"));
        assertEquals("machine learning and ml",
                TranscriptCorrector.correct("machine learning and ml"));
    }

    @Test
    void mediumConfidenceReplacesWhenNotInWhitelist() {
        assertEquals("ML is fun", TranscriptCorrector.correct("ml is fun"));
    }

    @Test
    void coreTermsAreProtected() {
        assertEquals("the api is ready", TranscriptCorrector.correct("the api is ready"));
    }

    @Test
    void addTermsHandlesNullAndBlank() {
        TranscriptCorrector.addTerms(null);
        TranscriptCorrector.addTerms(List.of("", "  "));
        assertEquals(0, TranscriptCorrector.whitelistSize());
    }

    @Test
    void clearWhitelistRemovesAllTerms() {
        TranscriptCorrector.addTerms(List.of("AI", "ML"));
        assertEquals(2, TranscriptCorrector.whitelistSize());
        TranscriptCorrector.clearWhitelist();
        assertEquals(0, TranscriptCorrector.whitelistSize());
    }
}
