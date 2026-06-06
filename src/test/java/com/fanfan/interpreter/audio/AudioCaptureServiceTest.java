package com.fanfan.interpreter.audio;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AudioCaptureServiceTest {
    @Test
    void listsAudioSourcesWithoutThrowing() {
        List<AudioSource> sources = assertDoesNotThrow(AudioCaptureService::listSources);

        if (isWindows() && !sources.isEmpty()) {
            assertTrue(sources.stream().anyMatch(source -> source.backend() == AudioSource.Backend.WASAPI_LOOPBACK));
        }
    }

    @Test
    void usesWideStringForWasapiDeviceId() {
        Object argument = WasapiLoopbackCapture.deviceIdArgument("{device-id}");

        assertTrue(argument instanceof com.sun.jna.WString);
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("windows");
    }
}
