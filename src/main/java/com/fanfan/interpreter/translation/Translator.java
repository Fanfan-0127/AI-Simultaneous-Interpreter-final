package com.fanfan.interpreter.translation;

public interface Translator {
    /**
     * Translate text to the target language.
     *
     * @param text          the source text (any language — MT uses "auto" detection)
     * @param targetLangMt  the target language in MT API naming (e.g. "Chinese", "Japanese")
     * @return translated text
     */
    String translate(String text, String targetLangMt) throws Exception;

    /**
     * Refine a draft translation. Default delegates to {@link #translate}.
     */
    default String refine(String text, String draft, String targetLangMt) throws Exception {
        return translate(text, targetLangMt);
    }
}
