package com.fanfan.interpreter.asr;

@FunctionalInterface
public interface AsrListener {
    void onTranscript(String text, boolean finalResult);
}
