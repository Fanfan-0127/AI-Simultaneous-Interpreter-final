package com.fanfan.interpreter.config;

import com.fanfan.interpreter.App;

import javax.swing.SwingUtilities;
import java.io.File;

/**
 * Restarts the application by launching a new process and exiting.
 *
 * <p>Detects whether we are running from a jpackage app image
 * ({@code jpackage.app-path} is set) or from a plain JAR / IDE,
 * and re-launches accordingly.</p>
 */
public final class Restarter {

    private Restarter() {}

    /**
     * Schedules a restart on the AWT event thread, then exits.
     *
     * <p>The restart runs on the EDT so that pending UI events (dialog
     * dispose, window-close listeners) can drain before exit.</p>
     */
    public static void restart() {
        SwingUtilities.invokeLater(() -> {
            try {
                launchNewInstance();
            } catch (Exception ignored) {
                // If launch fails the user can still start the app manually.
            }
            System.exit(0);
        });
    }

    private static void launchNewInstance() throws Exception {
        String appPath = System.getProperty("jpackage.app-path");
        if (appPath != null && !appPath.isBlank()) {
            launchAppImage(appPath);
        } else {
            launchFromJar();
        }
    }

    private static void launchAppImage(String appPath) throws Exception {
        new ProcessBuilder(appPath).inheritIO().start();
    }

    private static void launchFromJar() throws Exception {
        String javaHome = System.getProperty("java.home");
        String javaBin = javaHome + File.separator + "bin" + File.separator + "java";
        String classpath = System.getProperty("java.class.path");
        new ProcessBuilder(javaBin, "-cp", classpath, App.class.getName())
                .inheritIO()
                .start();
    }
}
