package com.fanfan.interpreter.audio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AudioLevelAnalyzerTest {
    @Test
    void detectsSilentAudio() {
        byte[] chunk = new byte[320];

        AudioLevel level = AudioLevelAnalyzer.analyzePcm16LittleEndian(chunk, chunk.length);

        assertEquals(AudioLevel.Quality.SILENT, level.quality());
    }

    @Test
    void detectsClippingAudio() {
        byte[] chunk = new byte[] {(byte) 0xff, 0x7f, 0x00, (byte) 0x80};

        AudioLevel level = AudioLevelAnalyzer.analyzePcm16LittleEndian(chunk, chunk.length);

        assertEquals(AudioLevel.Quality.CLIPPING, level.quality());
    }
}
