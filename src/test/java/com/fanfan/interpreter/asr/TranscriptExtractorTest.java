package com.fanfan.interpreter.asr;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TranscriptExtractorTest {
    @Test
    void extractsNestedTranscriptText() {
        var event = JsonParser.parseString("""
                {
                  "type": "response.audio_transcript.delta",
                  "response": {
                    "output": [
                      {
                        "content": [
                          { "transcript": "hello world" }
                        ]
                      }
                    ]
                  }
                }
                """).getAsJsonObject();

        var transcript = TranscriptExtractor.extract(event).orElseThrow();

        assertEquals("hello world", transcript.text());
        assertTrue(!transcript.finalResult());
    }

    @Test
    void marksCompletedEventsAsFinal() {
        var event = JsonParser.parseString("""
                {
                  "type": "response.audio_transcript.completed",
                  "text": "final sentence"
                }
                """).getAsJsonObject();

        var transcript = TranscriptExtractor.extract(event).orElseThrow();

        assertEquals("final sentence", transcript.text());
        assertTrue(transcript.finalResult());
    }

    @Test
    void supportsDashScopeStyleNestedDelta() {
        var event = JsonParser.parseString("""
                {
                  "type": "conversation.item.input_audio_transcription.delta",
                  "item": {
                    "content": [
                      { "delta": "streaming words" }
                    ]
                  }
                }
                """).getAsJsonObject();

        var transcript = TranscriptExtractor.extract(event).orElseThrow();

        assertEquals("streaming words", transcript.text());
        assertTrue(!transcript.finalResult());
    }
}
