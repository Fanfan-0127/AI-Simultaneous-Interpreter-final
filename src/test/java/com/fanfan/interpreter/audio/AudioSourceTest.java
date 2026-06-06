package com.fanfan.interpreter.audio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AudioSourceTest {
    @Test
    void labelsLoopbackCandidates() {
        AudioSource source = new AudioSource("Stereo Mix - Realtek", null, true);

        assertTrue(source.toString().startsWith("[系统回采候选]"));
    }

    @Test
    void labelsNativeWasapiLoopbackSources() {
        AudioSource source = AudioSource.wasapiLoopback("Speakers", "device-id");

        assertEquals(AudioSource.Backend.WASAPI_LOOPBACK, source.backend());
        assertTrue(source.toString().startsWith("[原生 WASAPI 回采]"));
    }
}
