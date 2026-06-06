package com.fanfan.interpreter.audio;

import javax.sound.sampled.Mixer;

public record AudioSource(String label, Mixer.Info mixerInfo, boolean loopbackCandidate, Backend backend, String wasapiDeviceId) {
    public enum Backend {
        WASAPI_LOOPBACK,
        JAVA_SOUND
    }

    public AudioSource(String label, Mixer.Info mixerInfo, boolean loopbackCandidate) {
        this(label, mixerInfo, loopbackCandidate, Backend.JAVA_SOUND, null);
    }

    public AudioSource(String label, Mixer.Info mixerInfo, boolean loopbackCandidate, Backend backend) {
        this(label, mixerInfo, loopbackCandidate, backend, null);
    }

    public static AudioSource wasapiLoopback(String label, String deviceId) {
        return new AudioSource(label, null, true, Backend.WASAPI_LOOPBACK, deviceId);
    }

    @Override
    public String toString() {
        if (backend == Backend.WASAPI_LOOPBACK) {
            return "[原生 WASAPI 回采] " + label;
        }
        return loopbackCandidate ? "[系统回采候选] " + label : label;
    }
}
