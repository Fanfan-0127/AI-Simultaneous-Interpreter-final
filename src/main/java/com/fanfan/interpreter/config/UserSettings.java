package com.fanfan.interpreter.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class UserSettings {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path SETTINGS_FILE = Paths.get(
            System.getProperty("user.home"), ".fanfan-interpreter", "settings.json");

    @SerializedName("api_key")
    private String apiKey = "";

    @SerializedName("realtime_url")
    private String realtimeUrl = "wss://dashscope.aliyuncs.com/api-ws/v1/realtime";

    @SerializedName("asr_model")
    private String asrModel = "qwen3-asr-flash-realtime";

    @SerializedName("mt_model")
    private String mtModel = "qwen-mt-flash";

    @SerializedName("asr_language")
    private String asrLanguage = "en";

    @SerializedName("asr_sample_rate")
    private int asrSampleRate = 16000;

    @SerializedName("asr_vad_threshold")
    private float asrVadThreshold;

    @SerializedName("asr_vad_silence_ms")
    private int asrVadSilenceMs = 800;

    @SerializedName("asr_stability_delay_ms")
    private int asrStabilityDelayMs = 300;

    @SerializedName("draft_delay_ms")
    private long draftDelayMs = 180;

    @SerializedName("floating_source_color")
    private String floatingSourceColor = "#FFFFFF";

    @SerializedName("floating_translation_color")
    private String floatingTranslationColor = "#FFE696";

    @SerializedName("floating_source_font_size")
    private int floatingSourceFontSize = 26;

    @SerializedName("floating_translation_font_size")
    private int floatingTranslationFontSize = 28;

    @SerializedName("floating_source_font")
    private String floatingSourceFont = "SansSerif";

    @SerializedName("floating_translation_font")
    private String floatingTranslationFont = "SansSerif";

    @SerializedName("floating_line_spacing")
    private int floatingLineSpacing = 4;

    @SerializedName("floating_bg_opacity")
    private int floatingBgOpacity = 180;

    @SerializedName("theme")
    private String theme = "dark";

    @SerializedName("floating_window_x")
    private int floatingWindowX = -1;

    @SerializedName("floating_window_y")
    private int floatingWindowY = -1;

    @SerializedName("main_window_x")
    private int mainWindowX = -1;

    @SerializedName("main_window_y")
    private int mainWindowY = -1;

    @SerializedName("main_window_width")
    private int mainWindowWidth = -1;

    @SerializedName("main_window_height")
    private int mainWindowHeight = -1;

    public static UserSettings load() {
        return loadFrom(SETTINGS_FILE);
    }

    static UserSettings loadFrom(Path path) {
        if (Files.isRegularFile(path)) {
            try {
                String json = Files.readString(path);
                UserSettings loaded = GSON.fromJson(json, UserSettings.class);
                if (loaded != null) {
                    return loaded;
                }
            } catch (IOException ignored) {
                // Corrupted or unreadable file — fall back to defaults
            }
        }
        return new UserSettings();
    }

    public void save() throws IOException {
        saveTo(SETTINGS_FILE);
    }

    void saveTo(Path path) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, GSON.toJson(this));
    }

    public AppConfig toAppConfig() {
        return new AppConfig(
                envOrValue("DASHSCOPE_API_KEY", apiKey),
                envOrValue("DASHSCOPE_REALTIME_URL", realtimeUrl),
                envOrValue("DASHSCOPE_REALTIME_MODEL", asrModel),
                envOrValue("DASHSCOPE_MT_MODEL", mtModel),
                envOrValue("ASR_LANGUAGE", asrLanguage),
                intEnvOrValue("ASR_SAMPLE_RATE", asrSampleRate),
                floatEnvOrValue("ASR_VAD_THRESHOLD", asrVadThreshold),
                intEnvOrValue("ASR_VAD_SILENCE_MS", asrVadSilenceMs),
                intEnvOrValue("ASR_STABILITY_DELAY_MS", asrStabilityDelayMs)
        );
    }

    // ---- accessors ----

    public String apiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey != null ? apiKey : ""; }

    public String realtimeUrl() { return realtimeUrl; }
    public void setRealtimeUrl(String realtimeUrl) { this.realtimeUrl = realtimeUrl; }

    public String asrModel() { return asrModel; }
    public void setAsrModel(String asrModel) { this.asrModel = asrModel; }

    public String mtModel() { return mtModel; }
    public void setMtModel(String mtModel) { this.mtModel = mtModel; }

    public String asrLanguage() { return asrLanguage; }
    public void setAsrLanguage(String asrLanguage) { this.asrLanguage = asrLanguage; }

    public int asrSampleRate() { return asrSampleRate; }
    public void setAsrSampleRate(int asrSampleRate) { this.asrSampleRate = asrSampleRate; }

    public float asrVadThreshold() { return asrVadThreshold; }
    public void setAsrVadThreshold(float asrVadThreshold) { this.asrVadThreshold = asrVadThreshold; }

    public int asrVadSilenceMs() { return asrVadSilenceMs; }
    public void setAsrVadSilenceMs(int asrVadSilenceMs) { this.asrVadSilenceMs = asrVadSilenceMs; }

    public int asrStabilityDelayMs() { return asrStabilityDelayMs; }
    public void setAsrStabilityDelayMs(int asrStabilityDelayMs) { this.asrStabilityDelayMs = asrStabilityDelayMs; }

    public long draftDelayMs() { return draftDelayMs; }
    public void setDraftDelayMs(long draftDelayMs) { this.draftDelayMs = draftDelayMs; }

    public String floatingSourceColor() { return floatingSourceColor; }
    public void setFloatingSourceColor(String floatingSourceColor) { this.floatingSourceColor = floatingSourceColor; }

    public String floatingTranslationColor() { return floatingTranslationColor; }
    public void setFloatingTranslationColor(String floatingTranslationColor) {
        this.floatingTranslationColor = floatingTranslationColor;
    }

    public int floatingSourceFontSize() { return floatingSourceFontSize; }
    public void setFloatingSourceFontSize(int floatingSourceFontSize) {
        this.floatingSourceFontSize = floatingSourceFontSize;
    }

    public int floatingTranslationFontSize() { return floatingTranslationFontSize; }
    public void setFloatingTranslationFontSize(int floatingTranslationFontSize) {
        this.floatingTranslationFontSize = floatingTranslationFontSize;
    }

    public int floatingLineSpacing() { return floatingLineSpacing; }
    public void setFloatingLineSpacing(int floatingLineSpacing) {
        this.floatingLineSpacing = floatingLineSpacing;
    }

    public String floatingSourceFont() { return floatingSourceFont; }
    public void setFloatingSourceFont(String floatingSourceFont) {
        this.floatingSourceFont = floatingSourceFont != null ? floatingSourceFont : "SansSerif";
    }

    public String floatingTranslationFont() { return floatingTranslationFont; }
    public void setFloatingTranslationFont(String floatingTranslationFont) {
        this.floatingTranslationFont = floatingTranslationFont != null ? floatingTranslationFont : "SansSerif";
    }

    public int floatingBgOpacity() { return floatingBgOpacity; }
    public void setFloatingBgOpacity(int floatingBgOpacity) {
        this.floatingBgOpacity = Math.max(0, Math.min(floatingBgOpacity, 255));
    }

    public String theme() { return theme; }
    public void setTheme(String theme) { this.theme = theme != null ? theme : "dark"; }

    public int floatingWindowX() { return floatingWindowX; }
    public void setFloatingWindowX(int floatingWindowX) { this.floatingWindowX = floatingWindowX; }

    public int floatingWindowY() { return floatingWindowY; }
    public void setFloatingWindowY(int floatingWindowY) { this.floatingWindowY = floatingWindowY; }

    public int mainWindowX() { return mainWindowX; }
    public void setMainWindowX(int mainWindowX) { this.mainWindowX = mainWindowX; }

    public int mainWindowY() { return mainWindowY; }
    public void setMainWindowY(int mainWindowY) { this.mainWindowY = mainWindowY; }

    public int mainWindowWidth() { return mainWindowWidth; }
    public void setMainWindowWidth(int mainWindowWidth) { this.mainWindowWidth = mainWindowWidth; }

    public int mainWindowHeight() { return mainWindowHeight; }
    public void setMainWindowHeight(int mainWindowHeight) { this.mainWindowHeight = mainWindowHeight; }

    // ---- helpers ----

    private static String envOrValue(String envKey, String jsonValue) {
        String env = System.getenv(envKey);
        return env != null && !env.isBlank() ? env : jsonValue;
    }

    private static int intEnvOrValue(String envKey, int jsonValue) {
        String env = System.getenv(envKey);
        if (env != null && !env.isBlank()) {
            try {
                return Integer.parseInt(env);
            } catch (NumberFormatException ignored) {
            }
        }
        return jsonValue;
    }

    private static float floatEnvOrValue(String envKey, float jsonValue) {
        String env = System.getenv(envKey);
        if (env != null && !env.isBlank()) {
            try {
                return Float.parseFloat(env);
            } catch (NumberFormatException ignored) {
            }
        }
        return jsonValue;
    }
}
