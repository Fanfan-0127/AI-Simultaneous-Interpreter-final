package com.fanfan.interpreter.ui;

import javax.swing.UIManager;
import java.awt.Color;

/**
 * Centralized color palette and FlatLaf UIManager defaults.
 *
 * <p>All color constants are applied via {@link #applyDefaults()} at startup
 * so that every component inherits a consistent, modern dark theme.</p>
 */
public final class Theme {

    // ---- Surface colors ----
    public static final Color BG = new Color(0x1E1E2E);           // deep navy-dark
    public static final Color CARD = new Color(0x282840);         // card / elevated surface
    public static final Color BORDER = new Color(0x3E3E5C);      // subtle border

    // ---- Text colors ----
    public static final Color TEXT_PRIMARY = new Color(0xE0E0F0);
    public static final Color TEXT_SECONDARY = new Color(0xA0A0C0);
    public static final Color TEXT_MUTED = new Color(0x6E6E8A);

    // ---- Accent ----
    public static final Color ACCENT = new Color(0x7DCFFF);       // bright cyan-blue
    public static final Color ACCENT_DIM = new Color(0x5BA4D6);

    // ---- Status ----
    public static final Color STATUS_SUCCESS = new Color(0x5AE0AA);
    public static final Color STATUS_WARNING = new Color(0xF0C060);
    public static final Color STATUS_ERROR = new Color(0xF06078);
    public static final Color STATUS_INFO = new Color(0x60B8F0);

    // ---- Floating window ----
    public static final Color FLOAT_BG = new Color(0, 0, 0, 160);
    public static final Color FLOAT_SHADOW = new Color(0, 0, 0, 64);

    private Theme() {}

    /** Apply FlatLaf UIManager overrides for our custom palette. */
    public static void applyDefaults() {
        UIManager.put("Button.arc", 8);
        UIManager.put("Component.arc", 8);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("TextComponent.arc", 8);
        UIManager.put("ProgressBar.arc", 8);

        UIManager.put("Table.showHorizontalLines", true);
        UIManager.put("Table.showVerticalLines", false);
    }
}
