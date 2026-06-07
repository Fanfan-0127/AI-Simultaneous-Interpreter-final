package com.fanfan.interpreter.translation;

import java.util.function.Consumer;

public interface Translator {
    String translateEnglishToChinese(String englishText) throws Exception;

    default String refineEnglishToChinese(String englishText, String draftChineseText) throws Exception {
        return translateEnglishToChinese(englishText);
    }

    default String translateEnglishToChineseStreaming(
            String englishText,
            Consumer<String> onToken,
            Consumer<String> onComplete,
            Consumer<Exception> onError
    ) throws Exception {
        String result = translateEnglishToChinese(englishText);
        onToken.accept(result);
        onComplete.accept(result);
        return result;
    }
}
