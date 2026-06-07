package com.fanfan.interpreter.audio;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class AudioCaptureService implements AutoCloseable {
    public static final AudioFormat PCM_16K_MONO = new AudioFormat(16000.0f, 16, 1, true, false);
    private static final int CHUNK_BYTES = 1600;

    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "audio-capture");
        thread.setDaemon(true);
        return thread;
    });

    private volatile TargetDataLine line;
    private volatile WasapiLoopbackCapture wasapiCapture;
    private volatile Future<?> captureTask;

    public static List<AudioSource> listSources() {
        List<AudioSource> sources = new ArrayList<>();
        if (isWindows()) {
            sources.addAll(WasapiLoopbackCapture.listSources());
        }
        DataLine.Info targetInfo = new DataLine.Info(TargetDataLine.class, PCM_16K_MONO);
        for (Mixer.Info mixerInfo : AudioSystem.getMixerInfo()) {
            Mixer mixer = AudioSystem.getMixer(mixerInfo);
            if (mixer.isLineSupported(targetInfo)) {
                String label = mixerInfo.getName() + " - " + mixerInfo.getDescription();
                sources.add(new AudioSource(label, mixerInfo, isLoopbackCandidate(label), AudioSource.Backend.JAVA_SOUND));
            }
        }
        sources.sort(Comparator.comparing((AudioSource source) -> source.backend() == AudioSource.Backend.WASAPI_LOOPBACK).reversed()
                .thenComparing(Comparator.comparing(AudioSource::loopbackCandidate).reversed())
                .thenComparing(AudioSource::label));
        return sources;
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("windows");
    }

    private static boolean isLoopbackCandidate(String label) {
        String normalized = label.toLowerCase();
        return normalized.contains("stereo mix") || normalized.contains("what u hear")
                || normalized.contains("what you hear") || normalized.contains("loopback")
                || normalized.contains("wasapi") || normalized.contains("speaker")
                || normalized.contains("speakers") || normalized.contains("扬声器")
                || normalized.contains("立体声混音") || normalized.contains("回放")
                || normalized.contains("回采");
    }

    public synchronized void start(AudioSource source, Consumer<byte[]> chunkConsumer) throws LineUnavailableException {
        start(source, chunkConsumer, (chunk, level) -> {});
    }

    public synchronized void start(AudioSource source, Consumer<byte[]> chunkConsumer,
                                    BiConsumer<byte[], AudioLevel> monitoredChunkConsumer) throws LineUnavailableException {
        Objects.requireNonNull(source, "source");
        stop();
        if (source.backend() == AudioSource.Backend.WASAPI_LOOPBACK) {
            WasapiLoopbackCapture nextCapture = new WasapiLoopbackCapture(source.wasapiDeviceId());
            wasapiCapture = nextCapture;
            CompletableFuture<Void> started = new CompletableFuture<>();
            captureTask = executor.submit(() -> {
                try {
                    nextCapture.capture(chunkConsumer, monitoredChunkConsumer, () -> started.complete(null));
                } catch (LineUnavailableException exception) {
                    started.completeExceptionally(exception);
                    throw new IllegalStateException(exception);
                }
            });
            awaitWasapiStart(started);
            return;
        }
        Mixer mixer = AudioSystem.getMixer(source.mixerInfo());
        DataLine.Info targetInfo = new DataLine.Info(TargetDataLine.class, PCM_16K_MONO);
        TargetDataLine nextLine = (TargetDataLine) mixer.getLine(targetInfo);
        nextLine.open(PCM_16K_MONO);
        nextLine.start();
        line = nextLine;
        captureTask = executor.submit(() -> captureLoop(nextLine, chunkConsumer, monitoredChunkConsumer));
    }

    private static void awaitWasapiStart(CompletableFuture<Void> started) throws LineUnavailableException {
        try {
            started.get(5, TimeUnit.SECONDS);
        } catch (TimeoutException exception) {
            throw new LineUnavailableException("WASAPI loopback start timed out");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new LineUnavailableException("Interrupted while starting WASAPI loopback");
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof LineUnavailableException lue) throw lue;
            throw new LineUnavailableException(cause == null ? exception.getMessage() : cause.getMessage());
        }
    }

    public synchronized void stop() {
        TargetDataLine currentLine = line;
        WasapiLoopbackCapture currentWasapi = wasapiCapture;
        line = null;
        wasapiCapture = null;
        if (captureTask != null) {
            captureTask.cancel(true);
            captureTask = null;
        }
        if (currentWasapi != null) currentWasapi.close();
        if (currentLine != null) {
            currentLine.stop();
            currentLine.close();
        }
    }

    @Override
    public void close() {
        stop();
        executor.shutdownNow();
    }

    private static void captureLoop(TargetDataLine currentLine, Consumer<byte[]> chunkConsumer,
                                     BiConsumer<byte[], AudioLevel> monitoredChunkConsumer) {
        byte[] buffer = new byte[CHUNK_BYTES];
        while (!Thread.currentThread().isInterrupted() && currentLine.isOpen()) {
            int read = currentLine.read(buffer, 0, buffer.length);
            if (read > 0) {
                byte[] chunk = new byte[read];
                System.arraycopy(buffer, 0, chunk, 0, read);
                chunkConsumer.accept(chunk);
                monitoredChunkConsumer.accept(chunk, AudioLevelAnalyzer.analyzePcm16LittleEndian(chunk, read));
            }
        }
    }
}
