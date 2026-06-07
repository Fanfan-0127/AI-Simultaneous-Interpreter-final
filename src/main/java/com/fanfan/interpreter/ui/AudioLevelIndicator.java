package com.fanfan.interpreter.ui;

import com.fanfan.interpreter.audio.AudioLevel;

import javax.swing.JComponent;
import javax.swing.Timer;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

/**
 * Compact audio-level bar that replaces the plain text status label.
 *
 * <p>Colors: green (normal), amber (low), red (clipping), gray (silent).
 * Transitions smoothly between levels with a short timer-driven animation.</p>
 */
final class AudioLevelIndicator extends JComponent {
    private static final int WIDTH = 80;
    private static final int HEIGHT = 12;
    private static final int ARC = 6;
    private static final float ANIM_SPEED = 0.25f;

    private AudioLevel.Quality currentQuality = AudioLevel.Quality.SILENT;
    private float[] currentColor = {0.4f, 0.4f, 0.4f}; // interpolated RGB
    private final Timer animTimer;

    AudioLevelIndicator() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setMinimumSize(new Dimension(WIDTH, HEIGHT));
        setMaximumSize(new Dimension(WIDTH, HEIGHT));
        animTimer = new Timer(30, event -> tick());
        animTimer.setRepeats(true);
    }

    void setLevel(AudioLevel.Quality quality) {
        if (quality == currentQuality) return;
        currentQuality = quality;
        if (!animTimer.isRunning()) animTimer.start();
    }

    private void tick() {
        float[] target = colorFor(currentQuality);
        boolean converged = true;
        for (int i = 0; i < 3; i++) {
            float delta = target[i] - currentColor[i];
            if (Math.abs(delta) < 0.005f) {
                currentColor[i] = target[i];
            } else {
                currentColor[i] += delta * ANIM_SPEED;
                converged = false;
            }
        }
        repaint();
        if (converged) animTimer.stop();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            // background track
            g2d.setColor(new Color(0x3E3E5C));
            g2d.fill(new RoundRectangle2D.Double(0, 0, WIDTH, HEIGHT, ARC, ARC));
            // level bar
            Color barColor = new Color(
                    clamp(currentColor[0]), clamp(currentColor[1]), clamp(currentColor[2]));
            g2d.setColor(barColor);
            g2d.fill(new RoundRectangle2D.Double(0, 0, WIDTH, HEIGHT, ARC, ARC));
            // gloss highlight
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
            g2d.setColor(Color.WHITE);
            g2d.fill(new RoundRectangle2D.Double(2, 2, WIDTH - 4, HEIGHT / 2f, ARC - 2, ARC - 2));
        } finally {
            g2d.dispose();
        }
    }

    private static float[] colorFor(AudioLevel.Quality quality) {
        return switch (quality) {
            case NORMAL -> new float[]{0.353f, 0.878f, 0.667f};  // green
            case LOW -> new float[]{0.941f, 0.753f, 0.376f};     // amber
            case CLIPPING -> new float[]{0.941f, 0.376f, 0.471f}; // red
            case SILENT -> new float[]{0.435f, 0.435f, 0.553f};   // gray
        };
    }

    private static float clamp(float v) { return Math.max(0f, Math.min(1f, v)); }
}
