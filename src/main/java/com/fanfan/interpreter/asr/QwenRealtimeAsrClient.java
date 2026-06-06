package com.fanfan.interpreter.asr;

import com.alibaba.dashscope.audio.omni.*;
import com.fanfan.interpreter.config.AppConfig;
import com.google.gson.JsonObject;
import java.util.Base64;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

public final class QwenRealtimeAsrClient implements AsrClient {
    private final AppConfig config;
    private final AtomicBoolean started = new AtomicBoolean(false);
    private volatile OmniRealtimeConversation conversation;

    public QwenRealtimeAsrClient(AppConfig config) { this.config = config; }

    @Override
    public void start(AsrListener listener) throws Exception {
        if (!config.hasApiKey()) throw new IllegalStateException("DASHSCOPE_API_KEY is not set.");
        OmniRealtimeParam param = OmniRealtimeParam.builder()
                .model(config.asrModel()).url(config.realtimeUrl()).apikey(config.apiKey()).build();
        conversation = new OmniRealtimeConversation(param, new OmniRealtimeCallback() {
            @Override public void onOpen() {}
            @Override public void onEvent(JsonObject message) {
                TranscriptExtractor.extract(message)
                        .ifPresent(t -> listener.onTranscript(t.text(), t.finalResult()));
            }
            @Override public void onClose(int code, String reason) {}
        });
        conversation.connect();
        conversation.updateSession(buildConfig(config));
        started.set(true);
    }

    @Override
    public void appendPcm(byte[] pcmChunk) {
        OmniRealtimeConversation current = conversation;
        if (!started.get() || current == null || pcmChunk.length == 0) return;
        current.appendAudio(Base64.getEncoder().encodeToString(pcmChunk));
    }

    @Override
    public void finish() throws Exception {
        OmniRealtimeConversation current = conversation;
        if (current != null) current.endSession();
    }

    @Override
    public void close() {
        started.set(false);
        OmniRealtimeConversation current = conversation;
        conversation = null;
        if (current != null) current.close();
    }

    private static OmniRealtimeConfig buildConfig(AppConfig config) {
        OmniRealtimeTranscriptionParam tp = new OmniRealtimeTranscriptionParam();
        tp.setLanguage(config.asrLanguage());
        tp.setInputSampleRate(config.asrSampleRate());
        tp.setInputAudioFormat("pcm");
        return OmniRealtimeConfig.builder()
                .modalities(Collections.singletonList(OmniRealtimeModality.TEXT))
                .enableTurnDetection(true).turnDetectionType("server_vad")
                .turnDetectionThreshold(config.asrVadThreshold())
                .turnDetectionSilenceDurationMs(config.asrVadSilenceDurationMs())
                .transcriptionConfig(tp).build();
    }
}
