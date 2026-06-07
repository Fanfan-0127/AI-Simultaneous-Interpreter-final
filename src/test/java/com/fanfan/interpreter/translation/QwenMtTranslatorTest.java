package com.fanfan.interpreter.translation;

import com.fanfan.interpreter.config.AppConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QwenMtTranslatorTest {

    private static final String TARGET_ZH = "Chinese";

    @Test
    void cacheKey_normalizesWhitespace() {
        assertEquals("hello world|" + TARGET_ZH,
                QwenMtTranslator.cacheKey("  Hello World  ", TARGET_ZH));
    }

    @Test
    void cacheKey_normalizesCase() {
        assertEquals("hello world|" + TARGET_ZH,
                QwenMtTranslator.cacheKey("HELLO WORLD", TARGET_ZH));
    }

    @Test
    void cacheKey_nullReturnsLangOnly() {
        assertEquals("|" + TARGET_ZH, QwenMtTranslator.cacheKey(null, TARGET_ZH));
    }

    @Test
    void cacheKey_blankReturnsLangOnly() {
        assertEquals("|" + TARGET_ZH, QwenMtTranslator.cacheKey("   ", TARGET_ZH));
    }

    @Test
    void translate_usesCacheWhenPopulated() throws Exception {
        var translator = new QwenMtTranslator(fakeConfig());
        translator.translationCache.put(QwenMtTranslator.cacheKey("cached text", TARGET_ZH), "缓存结果");
        String result = translator.translate("cached text", TARGET_ZH);
        assertEquals("缓存结果", result);
    }

    @Test
    void translate_cacheIsCaseInsensitive() throws Exception {
        var translator = new QwenMtTranslator(fakeConfig());
        translator.translationCache.put(QwenMtTranslator.cacheKey("hello world", TARGET_ZH), "你好世界");
        String result = translator.translate("HELLO WORLD", TARGET_ZH);
        assertEquals("你好世界", result);
        result = translator.translate("  hello world  ", TARGET_ZH);
        assertEquals("你好世界", result);
    }

    @Test
    void cacheEvictsOldestWhenFull() {
        var translator = new QwenMtTranslator(fakeConfig());
        // Fill cache beyond CACHE_MAX_SIZE (200)
        for (int i = 0; i < 250; i++) {
            translator.translationCache.put("key-" + i, "value-" + i);
        }
        // Oldest entries should be evicted, size capped at 200
        assertFalse(translator.translationCache.containsKey("key-0"),
                "First inserted entry should be evicted when cache exceeds max size");
        assertTrue(translator.translationCache.containsKey("key-249"),
                "Most recently accessed entry should remain");
    }

    private static AppConfig fakeConfig() {
        return new AppConfig(
                null, "wss://dashscope.aliyuncs.com/api-ws/v1/realtime",
                "qwen3-asr-flash-realtime", "qwen-mt-flash",
                "en", "Chinese", 16000, 0.0f, 800, 700
        );
    }
}
