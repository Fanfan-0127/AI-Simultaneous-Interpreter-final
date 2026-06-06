package com.fanfan.interpreter.audio;

public final class AudioLevelAnalyzer {
    private static final double SILENT_RMS = 0.003;
    private static final double LOW_RMS = 0.02;
    private static final double CLIPPING_PEAK = 0.98;

    private AudioLevelAnalyzer() {}

    public static AudioLevel analyzePcm16LittleEndian(byte[] pcmChunk, int length) {
        if (pcmChunk == null || length < 2) {
            return new AudioLevel(0.0, 0.0, AudioLevel.Quality.SILENT);
        }
        int samples = length / 2;
        double sumSquares = 0.0;
        double peak = 0.0;
        for (int index = 0; index + 1 < length; index += 2) {
            int low = pcmChunk[index] & 0xff;
            int high = pcmChunk[index + 1];
            short sample = (short) ((high << 8) | low);
            double normalized = sample / 32768.0;
            sumSquares += normalized * normalized;
            peak = Math.max(peak, Math.abs(normalized));
        }
        double rms = Math.sqrt(sumSquares / samples);
        return new AudioLevel(rms, peak, quality(rms, peak));
    }

    private static AudioLevel.Quality quality(double rms, double peak) {
        if (peak >= CLIPPING_PEAK) return AudioLevel.Quality.CLIPPING;
        if (rms < SILENT_RMS) return AudioLevel.Quality.SILENT;
        if (rms < LOW_RMS) return AudioLevel.Quality.LOW;
        return AudioLevel.Quality.NORMAL;
    }
}
