package com.fanfan.interpreter.audio;

public record AudioLevel(double rms, double peak, Quality quality) {
    public enum Quality {
        SILENT,
        LOW,
        NORMAL,
        CLIPPING
    }
}
