package com.fanfan.interpreter.asr;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StableTranscriptSchedulerTest {
    @Test
    void emitsOnlyLatestPartialAfterStabilityDelay() throws Exception {
        List<String> emitted = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);
        try (StableTranscriptScheduler scheduler = new StableTranscriptScheduler(80)) {
            scheduler.accept("hello", false, (text, finalResult) -> emitted.add(text));
            scheduler.accept("hello world", false, (text, finalResult) -> {
                emitted.add(text);
                latch.countDown();
            });

            assertTrue(latch.await(1, TimeUnit.SECONDS));
        }

        assertEquals(List.of("hello world"), emitted);
    }

    @Test
    void finalTranscriptEmitsImmediately() {
        List<String> emitted = new ArrayList<>();
        try (StableTranscriptScheduler scheduler = new StableTranscriptScheduler(1000)) {
            scheduler.accept("final words", true, (text, finalResult) -> emitted.add(text + "|" + finalResult));
        }

        assertEquals(List.of("final words|true"), emitted);
    }
}
