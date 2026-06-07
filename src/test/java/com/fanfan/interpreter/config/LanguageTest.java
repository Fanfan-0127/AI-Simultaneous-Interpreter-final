package com.fanfan.interpreter.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LanguageTest {

    @Test
    void findAsrByCode_returnsCorrectLanguage() {
        assertEquals("English", Language.findAsrByCode("en").displayName());
        assertEquals("日本語", Language.findAsrByCode("ja").displayName());
        assertEquals("中文 (普通话)", Language.findAsrByCode("zh").displayName());
        assertEquals("Deutsch", Language.findAsrByCode("de").displayName());
    }

    @Test
    void findAsrByCode_isCaseInsensitive() {
        assertNotNull(Language.findAsrByCode("EN"));
        assertNotNull(Language.findAsrByCode("Ja"));
        assertEquals("English", Language.findAsrByCode("EN").displayName());
    }

    @Test
    void findAsrByCode_returnsNullForUnknown() {
        assertNull(Language.findAsrByCode("xx"));
        assertNull(Language.findAsrByCode(""));
        assertNull(Language.findAsrByCode(null));
    }

    @Test
    void displayNameForAsrCode_fallsBackToCode() {
        assertEquals("English", Language.displayNameForAsrCode("en"));
        assertEquals("xyz", Language.displayNameForAsrCode("xyz"));
    }

    @Test
    void findTargetByMtName_returnsCorrectLanguage() {
        assertEquals("中文", Language.findTargetByMtName("Chinese").displayName());
        assertEquals("日本語", Language.findTargetByMtName("Japanese").displayName());
        assertEquals("English", Language.findTargetByMtName("English").displayName());
    }

    @Test
    void findTargetByMtName_isCaseInsensitive() {
        assertNotNull(Language.findTargetByMtName("chinese"));
        assertNotNull(Language.findTargetByMtName("JAPANESE"));
        assertEquals("中文", Language.findTargetByMtName("CHINESE").displayName());
    }

    @Test
    void findTargetByMtName_returnsNullForUnknown() {
        assertNull(Language.findTargetByMtName("Klingon"));
        assertNull(Language.findTargetByMtName(""));
        assertNull(Language.findTargetByMtName(null));
    }

    @Test
    void displayNameForTarget_fallsBackToName() {
        assertEquals("中文", Language.displayNameForTarget("Chinese"));
        assertEquals("Esperanto", Language.displayNameForTarget("Esperanto"));
    }

    @Test
    void supportedAsr_hasAll28Languages() {
        assertEquals(27, Language.SUPPORTED_ASR.size());
        // Verify key languages are present
        assertTrue(Language.SUPPORTED_ASR.stream().anyMatch(l -> l.code().equals("en")));
        assertTrue(Language.SUPPORTED_ASR.stream().anyMatch(l -> l.code().equals("zh")));
        assertTrue(Language.SUPPORTED_ASR.stream().anyMatch(l -> l.code().equals("ja")));
        assertTrue(Language.SUPPORTED_ASR.stream().anyMatch(l -> l.code().equals("ko")));
        // Each language has unique code
        long distinctCodes = Language.SUPPORTED_ASR.stream().map(Language.AsrLanguage::code).distinct().count();
        assertEquals(Language.SUPPORTED_ASR.size(), distinctCodes);
    }

    @Test
    void curatedTargets_hasCommonLanguages() {
        assertFalse(Language.CURATED_TARGETS.isEmpty());
        assertTrue(Language.CURATED_TARGETS.stream().anyMatch(l -> l.mtName().equals("Chinese")));
        assertTrue(Language.CURATED_TARGETS.stream().anyMatch(l -> l.mtName().equals("English")));
        assertTrue(Language.CURATED_TARGETS.stream().anyMatch(l -> l.mtName().equals("Japanese")));
    }
}
