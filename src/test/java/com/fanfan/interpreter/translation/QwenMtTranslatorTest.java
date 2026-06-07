package com.fanfan.interpreter.translation;

import com.fanfan.interpreter.config.AppConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QwenMtTranslatorTest {

    @Test
    void cacheKey_normalizesWhitespace() {
        assertEquals("hello world", QwenMtTranslator.cacheKey("  Hello World  "));
    }

    @Test
    void cacheKey_normalizesCase() {
        assertEquals("hello world", QwenMtTranslator.cacheKey("HELLO WORLD"));
    }

    @Test
    void cacheKey_nullReturnsEmpty() {
        assertEquals("", QwenMtTranslator.cacheKey(null));
    }

    @Test
    void cacheKey_blankReturnsBlank() {
        assertTrue(QwenMtTranslator.cacheKey("   ").isEmpty());
    }

    @Test
    void translateEnglishToChinese_usesCacheWhenPopulated() throws Exception {
        var translator = new QwenMtTranslator(fakeConfig());
        translator.translationCache.put(QwenMtTranslator.cacheKey("cached text"), "缓存结果");
        String result = translator.translateEnglishToChinese("cached text");
        assertEquals("缓存结果", result);
    }

    @Test
    void translateEnglishToChinese_cacheIsCaseInsensitive() throws Exception {
        var translator = new QwenMtTranslator(fakeConfig());
        translator.translationCache.put(QwenMtTranslator.cacheKey("hello world"), "你好世界");
        String result = translator.translateEnglishToChinese("HELLO WORLD");
        assertEquals("你好世界", result);
        result = translator.translateEnglishToChinese("  hello world  ");
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
                "en", 16000, 0.0f, 800, 700
        );
    }
}
