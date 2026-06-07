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
    public String translate(String text, String targetLangMt) throws Exception {
        String key = cacheKey(text, targetLangMt);
        String cached = translationCache.get(key);
        if (cached != null) {
            return cached;
        }
        String result = callQwenMt(text, targetLangMt);
        if (!result.isEmpty()) {
            translationCache.put(key, result);
        }
        return result;
    }

    @Override
    public String refine(String text, String draft, String targetLangMt) throws Exception {
        return callQwenMt(text, targetLangMt);
    }

    static String cacheKey(String text, String targetLangMt) {
        String t = text == null ? "" : text.strip().toLowerCase(java.util.Locale.ROOT);
        String lang = targetLangMt == null ? "" : targetLangMt;
        return t + "|" + lang;
    }

    private String callQwenMt(String text, String targetLangMt) throws Exception {
        if (text == null || text.isBlank()) return "";
        if (!config.hasApiKey()) throw new IllegalStateException("DASHSCOPE_API_KEY is not set.");
        GenerationParam param = buildParam(text, targetLangMt);
        return extractText(generation.call(param));
    }

    private GenerationParam buildParam(String text, String targetLangMt) {
        Message message = Message.builder().role(Role.USER.getValue()).content(text.strip()).build();
        TranslationOptions options = TranslationOptions.builder()
                .sourceLang("auto").targetLang(targetLangMt)
                .domains("Technology, software engineering, AI, online meetings, conference talks").build();
        return GenerationParam.builder()
                .apiKey(config.apiKey()).model(config.mtModel())
                .messages(List.of(message)).translationOptions(options)
                .resultFormat(GenerationParam.ResultFormat.MESSAGE).build();
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
