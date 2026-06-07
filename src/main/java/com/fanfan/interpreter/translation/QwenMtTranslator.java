package com.fanfan.interpreter.translation;

import com.alibaba.dashscope.aigc.generation.*;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.fanfan.interpreter.config.AppConfig;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class QwenMtTranslator implements Translator {
    private static final int CACHE_MAX_SIZE = 200;
    private final AppConfig config;
    private final Generation generation = new Generation();
    final Map<String, String> translationCache =
            Collections.synchronizedMap(new LinkedHashMap<>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
                    return size() > CACHE_MAX_SIZE;
                }
            });

    public QwenMtTranslator(AppConfig config) { this.config = config; }

    @Override
    public String translateEnglishToChinese(String englishText) throws Exception {
        String key = cacheKey(englishText);
        String cached = translationCache.get(key);
        if (cached != null) {
            return cached;
        }
        String result = callQwenMt(englishText);
        if (!result.isEmpty()) {
            translationCache.put(key, result);
        }
        return result;
    }

    @Override
    public String refineEnglishToChinese(String englishText, String draftChineseText) throws Exception {
        return callQwenMt(englishText);
    }

    static String cacheKey(String text) {
        return text == null ? "" : text.strip().toLowerCase(java.util.Locale.ROOT);
    }

    private String callQwenMt(String englishText) throws Exception {
        if (!config.hasApiKey()) throw new IllegalStateException("DASHSCOPE_API_KEY is not set.");
        if (englishText == null || englishText.isBlank()) return "";
        Message message = Message.builder().role(Role.USER.getValue()).content(englishText.strip()).build();
        TranslationOptions options = TranslationOptions.builder()
                .sourceLang("English").targetLang("Chinese")
                .domains("Technology, software engineering, AI, online meetings, conference talks").build();
        GenerationParam param = GenerationParam.builder()
                .apiKey(config.apiKey()).model(config.mtModel())
                .messages(List.of(message)).translationOptions(options)
                .resultFormat(GenerationParam.ResultFormat.MESSAGE).build();
        return extractText(generation.call(param));
    }

    static String extractText(GenerationResult result) {
        if (result == null || result.getOutput() == null) return "";
        GenerationOutput output = result.getOutput();
        if (output.getChoices() != null && !output.getChoices().isEmpty()) {
            Message message = output.getChoices().getFirst().getMessage();
            if (message != null && message.getContent() != null) return message.getContent().strip();
        }
        return output.getText() == null ? "" : output.getText().strip();
    }
}
