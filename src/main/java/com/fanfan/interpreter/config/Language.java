package com.fanfan.interpreter.config;

import java.util.List;
import java.util.Objects;

/**
 * Language definitions for ASR source languages and MT target languages.
 *
 * <p>ASR uses short codes (e.g. "en", "ja") for the DashScope WebSocket API.
 * MT uses full English names (e.g. "English", "Japanese") for the TranslationOptions API.
 * MT source language is always "auto" — no mapping needed.
 */
public final class Language {

    private Language() {}

    // ── ASR source languages ──────────────────────────────────────────

    /**
     * An ASR language: short code for the DashScope API + human-readable display name.
     */
    public record AsrLanguage(String code, String displayName) {
        public AsrLanguage {
            Objects.requireNonNull(code, "code");
            Objects.requireNonNull(displayName, "displayName");
        }

        @Override
        public String toString() {
            return displayName + " (" + code + ")";
        }
    }

    /** All 28 languages supported by Qwen ASR Realtime. */
    public static final List<AsrLanguage> SUPPORTED_ASR = List.of(
            new AsrLanguage("zh",  "中文 (普通话)"),
            new AsrLanguage("yue", "粤语"),
            new AsrLanguage("en",  "English"),
            new AsrLanguage("ja",  "日本語"),
            new AsrLanguage("ko",  "한국어"),
            new AsrLanguage("de",  "Deutsch"),
            new AsrLanguage("fr",  "Français"),
            new AsrLanguage("es",  "Español"),
            new AsrLanguage("pt",  "Português"),
            new AsrLanguage("ru",  "Русский"),
            new AsrLanguage("ar",  "العربية"),
            new AsrLanguage("it",  "Italiano"),
            new AsrLanguage("hi",  "हिन्दी"),
            new AsrLanguage("id",  "Bahasa Indonesia"),
            new AsrLanguage("th",  "ไทย"),
            new AsrLanguage("tr",  "Türkçe"),
            new AsrLanguage("uk",  "Українська"),
            new AsrLanguage("vi",  "Tiếng Việt"),
            new AsrLanguage("cs",  "Čeština"),
            new AsrLanguage("da",  "Dansk"),
            new AsrLanguage("fil", "Filipino"),
            new AsrLanguage("fi",  "Suomi"),
            new AsrLanguage("is",  "Íslenska"),
            new AsrLanguage("ms",  "Bahasa Melayu"),
            new AsrLanguage("no",  "Norsk"),
            new AsrLanguage("pl",  "Polski"),
            new AsrLanguage("sv",  "Svenska")
    );

    /** Look up an ASR language by its short code. Returns null if not found. */
    public static AsrLanguage findAsrByCode(String code) {
        if (code == null || code.isBlank()) return null;
        String stripped = code.strip();
        for (AsrLanguage lang : SUPPORTED_ASR) {
            if (lang.code.equalsIgnoreCase(stripped)) return lang;
        }
        return null;
    }

    /** Get the display name for an ASR code, falling back to the code itself. */
    public static String displayNameForAsrCode(String code) {
        AsrLanguage lang = findAsrByCode(code);
        return lang != null ? lang.displayName() : code;
    }

    // ── MT target languages ───────────────────────────────────────────

    /**
     * A curated MT target language: full English name for the DashScope TranslationOptions API
     * + human-readable display name.
     */
    public record MtTargetLanguage(String mtName, String displayName) {
        public MtTargetLanguage {
            Objects.requireNonNull(mtName, "mtName");
            Objects.requireNonNull(displayName, "displayName");
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    /** Curated list of ~18 commonly used target languages. */
    public static final List<MtTargetLanguage> CURATED_TARGETS = List.of(
            new MtTargetLanguage("Chinese",          "中文"),
            new MtTargetLanguage("English",          "English"),
            new MtTargetLanguage("Japanese",         "日本語"),
            new MtTargetLanguage("Korean",           "한국어"),
            new MtTargetLanguage("German",           "Deutsch"),
            new MtTargetLanguage("French",           "Français"),
            new MtTargetLanguage("Spanish",          "Español"),
            new MtTargetLanguage("Portuguese",       "Português"),
            new MtTargetLanguage("Russian",          "Русский"),
            new MtTargetLanguage("Arabic",           "العربية"),
            new MtTargetLanguage("Italian",          "Italiano"),
            new MtTargetLanguage("Hindi",            "हिन्दी"),
            new MtTargetLanguage("Indonesian",       "Bahasa Indonesia"),
            new MtTargetLanguage("Thai",             "ไทย"),
            new MtTargetLanguage("Turkish",          "Türkçe"),
            new MtTargetLanguage("Vietnamese",       "Tiếng Việt"),
            new MtTargetLanguage("Dutch",            "Nederlands"),
            new MtTargetLanguage("Polish",           "Polski"),
            new MtTargetLanguage("Swedish",          "Svenska")
    );

    /** Look up a curated MT target language by its MT API name. */
    public static MtTargetLanguage findTargetByMtName(String mtName) {
        if (mtName == null || mtName.isBlank()) return null;
        String stripped = mtName.strip();
        for (MtTargetLanguage lang : CURATED_TARGETS) {
            if (lang.mtName.equalsIgnoreCase(stripped)) return lang;
        }
        return null;
    }

    /** Get the display name for an MT target name, falling back to the name itself. */
    public static String displayNameForTarget(String mtName) {
        MtTargetLanguage lang = findTargetByMtName(mtName);
        return lang != null ? lang.displayName() : mtName;
    }
}
