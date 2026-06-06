package com.fanfan.interpreter.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WindowsWindowHitTestControllerTest {
    private static final int WS_EX_LAYERED = 0x00080000;
    private static final int WS_EX_TRANSPARENT = 0x00000020;

    @Test
    void unlockedWindowKeepsLayeredStyleButRemovesMouseThrough() {
        int currentStyle = WS_EX_TRANSPARENT | 0x00000100;

        int style = WindowsWindowHitTestController.extendedStyle(currentStyle, false);

        assertEquals(WS_EX_LAYERED | 0x00000100, style);
    }

    @Test
    void lockedWindowEnablesLayeredMouseThroughStyle() {
        int currentStyle = 0x00000100;

        int style = WindowsWindowHitTestController.extendedStyle(currentStyle, true);

        assertEquals(WS_EX_LAYERED | WS_EX_TRANSPARENT | 0x00000100, style);
    }
}
