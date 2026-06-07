package com.fanfan.interpreter.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserSettingsTest {

    @TempDir
    Path tempDir;

    @Test
    void loadsDefaultsWhenNoFileExists() {
        Path nonExistent = tempDir.resolve("nonexistent.json");
        UserSettings settings = UserSettings.loadFrom(nonExistent);

        assertEquals("", settings.apiKey());
        assertEquals("wss://dashscope.aliyuncs.com/api-ws/v1/realtime", settings.realtimeUrl());
        assertEquals("qwen3-asr-flash-realtime", settings.asrModel());
        assertEquals("qwen-mt-flash", settings.mtModel());
        assertEquals("en", settings.asrLanguage());
        assertEquals("Chinese", settings.targetLanguage());
        assertEquals(16000, settings.asrSampleRate());
        assertEquals(800, settings.asrVadSilenceMs());
        assertEquals(300, settings.asrStabilityDelayMs());
        assertEquals(180, settings.draftDelayMs());
        assertEquals("#FFFFFF", settings.floatingSourceColor());
        assertEquals("#FFE696", settings.floatingTranslationColor());
        assertEquals(26, settings.floatingSourceFontSize());
        assertEquals(28, settings.floatingTranslationFontSize());
    }

    @Test
    void saveAndLoadRoundTrips() throws Exception {
        Path file = tempDir.resolve("settings.json");
        UserSettings original = new UserSettings();
        original.setApiKey("sk-test-key");
        original.setRealtimeUrl("wss://custom.example.com/ws");
        original.setAsrModel("custom-asr-model");
        original.setAsrLanguage("zh");
        original.setTargetLanguage("Japanese");
        original.setDraftDelayMs(300);
        original.setFloatingSourceColor("#00FF00");
        original.setFloatingSourceFontSize(32);

        original.saveTo(file);
        UserSettings loaded = UserSettings.loadFrom(file);

        assertEquals("sk-test-key", loaded.apiKey());
        assertEquals("wss://custom.example.com/ws", loaded.realtimeUrl());
        assertEquals("custom-asr-model", loaded.asrModel());
        assertEquals("zh", loaded.asrLanguage());
        assertEquals("Japanese", loaded.targetLanguage());
        assertEquals(300, loaded.draftDelayMs());
        assertEquals("#00FF00", loaded.floatingSourceColor());
        assertEquals(32, loaded.floatingSourceFontSize());
    }

    @Test
    void toAppConfigUsesJsonValuesForSettingsWithoutEnvOverrides() {
        UserSettings settings = new UserSettings();
        settings.setAsrLanguage("ja");
        settings.setAsrVadSilenceMs(2500);
        settings.setDraftDelayMs(500);

        AppConfig config = settings.toAppConfig();

        // These fields are unlikely to have env vars set, so JSON values should shine through
        assertEquals("ja", config.asrLanguage());
        assertEquals(2500, config.asrVadSilenceDurationMs());
    }

    @Test
    void toAppConfigHasNonNullValues() {
        UserSettings settings = new UserSettings();
        AppConfig config = settings.toAppConfig();

        assertNotNull(config.realtimeUrl());
        assertNotNull(config.asrModel());
        assertNotNull(config.mtModel());
        assertNotNull(config.asrLanguage());
        assertNotNull(config.targetLanguage());
    }

    @Test
    void saveCreatesParentDirectory(@TempDir Path anotherTempDir) throws Exception {
        Path nested = anotherTempDir.resolve("sub").resolve("nested").resolve("config.json");
        UserSettings settings = new UserSettings();
        settings.setDraftDelayMs(500);

        settings.saveTo(nested);

        assertTrue(java.nio.file.Files.isRegularFile(nested));
        UserSettings loaded = UserSettings.loadFrom(nested);
        assertEquals(500, loaded.draftDelayMs());
    }
}
