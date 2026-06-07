package com.fanfan.interpreter;

import com.fanfan.interpreter.ui.MainWindow;
import com.fanfan.interpreter.ui.Theme;
import com.formdev.flatlaf.FlatDarkLaf;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public final class App {
    private App() {
    }

    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        FlatDarkLaf.setup();
        Theme.applyDefaults();

        SwingUtilities.invokeLater(() -> {
            MainWindow window = new MainWindow();
            window.setVisible(true);
        });
    }
}
