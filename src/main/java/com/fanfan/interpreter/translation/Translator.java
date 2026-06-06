package com.fanfan.interpreter.translation;

public interface Translator {
    String translateEnglishToChinese(String englishText) throws Exception;
    default String refineEnglishToChinese(String englishText, String draftChineseText) throws Exception {
        return translateEnglishToChinese(englishText);
    }
}
