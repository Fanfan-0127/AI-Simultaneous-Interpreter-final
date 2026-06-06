package com.fanfan.interpreter.ui;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinUser;

import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.util.Locale;

final class WindowsWindowHitTestController {
    private static final int GWL_EXSTYLE = -20;
    private static final int WS_EX_LAYERED = 0x00080000;
    private static final int WS_EX_TRANSPARENT = 0x00000020;
    private static final int SWP_NOSIZE = 0x0001;
    private static final int SWP_NOMOVE = 0x0002;
    private static final int SWP_NOACTIVATE = 0x0010;
    private static final HWND HWND_TOPMOST = new HWND(Pointer.createConstant(-1));

    private WindowsWindowHitTestController() {
    }

    static void apply(Window window, boolean mouseThrough) {
        if (!isSupported()) {
            return;
        }
        HWND hwnd = hwnd(window);
        if (hwnd == null) {
            return;
        }
        int currentStyle = User32.INSTANCE.GetWindowLong(hwnd, GWL_EXSTYLE);
        User32.INSTANCE.SetWindowLong(hwnd, GWL_EXSTYLE, extendedStyle(currentStyle, mouseThrough));
        User32.INSTANCE.SetWindowPos(hwnd, HWND_TOPMOST, 0, 0, 0, 0, SWP_NOMOVE | SWP_NOSIZE | SWP_NOACTIVATE);
    }

    static int extendedStyle(int currentStyle, boolean mouseThrough) {
        int style = currentStyle | WS_EX_LAYERED;
        if (mouseThrough) {
            return style | WS_EX_TRANSPARENT;
        }
        return style & ~WS_EX_TRANSPARENT;
    }

    private static HWND hwnd(Window window) {
        try {
            Pointer pointer = Native.getWindowPointer(window);
            return pointer == null ? null : new HWND(pointer);
        } catch (IllegalStateException exception) {
            return null;
        }
    }

    private static boolean isSupported() {
        if (GraphicsEnvironment.isHeadless()) {
            return false;
        }
        return System.getProperty("os.name", "")
                .toLowerCase(Locale.ROOT)
                .contains("windows");
    }
}
