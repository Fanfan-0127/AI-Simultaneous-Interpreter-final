package com.fanfan.interpreter.translation;

import com.alibaba.dashscope.aigc.generation.*;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.fanfan.interpreter.config.AppConfig;
import java.util.List;

public final class QwenMtTranslator implements Translator {
    private final AppConfig config;
    private final Generation generation = new Generation();

    public QwenMtTranslator(AppConfig config) { this.config = config; }

    @Override
    public String translateEnglishToChinese(String englishText) throws Exception {
        return callQwenMt(englishText);
    }

    @Override
    public String refineEnglishToChinese(String englishText, String draftChineseText) throws Exception {
        return callQwenMt(englishText);
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
