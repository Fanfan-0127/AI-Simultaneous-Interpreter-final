package com.fanfan.interpreter.config;

public record AppConfig(
        String apiKey,
        String realtimeUrl,
        String asrModel,
        String mtModel,
        String asrLanguage,
        String targetLanguage,
        int asrSampleRate,
        float asrVadThreshold,
        int asrVadSilenceDurationMs,
        int asrStabilityDelayMs
) {
    public static AppConfig fromEnvironment() {
        return new AppConfig(
                System.getenv("DASHSCOPE_API_KEY"),
                envOrDefault("DASHSCOPE_REALTIME_URL", "wss://dashscope.aliyuncs.com/api-ws/v1/realtime"),
                envOrDefault("DASHSCOPE_REALTIME_MODEL", "qwen3-asr-flash-realtime"),
                envOrDefault("DASHSCOPE_MT_MODEL", "qwen-mt-flash"),
                envOrDefault("ASR_LANGUAGE", "en"),
                envOrDefault("TARGET_LANGUAGE", "Chinese"),
                intEnvOrDefault("ASR_SAMPLE_RATE", 16000),
                floatEnvOrDefault("ASR_VAD_THRESHOLD", 0.0f),
                intEnvOrDefault("ASR_VAD_SILENCE_MS", 800),
                intEnvOrDefault("ASR_STABILITY_DELAY_MS", 300)
        );
    }

    public static AppConfig fromSettings(UserSettings settings) {
        return settings.toAppConfig();
    }

    public boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }

    private static String envOrDefault(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }

    private static int intEnvOrDefault(String key, int fallback) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static float floatEnvOrDefault(String key, float fallback) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
