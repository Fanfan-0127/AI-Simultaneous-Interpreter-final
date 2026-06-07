package com.fanfan.interpreter.ui;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JWindow;
import javax.swing.Timer;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.Map;

public final class FloatingSubtitleWindow extends JWindow {
    private static final int WINDOW_WIDTH = 960;
    private static final int MIN_WINDOW_HEIGHT = 120;
    private static final int HORIZONTAL_PADDING = 64;
    private static final int VERTICAL_PADDING = 32;
    private static final int SCREEN_BOTTOM_MARGIN = 72;
    private static final int MAX_SOURCE_CHARACTERS = 140;
    private static final int MAX_TRANSLATION_CHARACTERS = 84;
    private static final int SOURCE_MAX_LINES = 3;
    private static final int TRANSLATION_MAX_LINES = 4;
    private static final int CORNER_RADIUS = 18;
    private static final float ANIM_DURATION_MS = 200f;

    private final JTextArea sourceText = new ShadowTextArea("等待识别结果...");
    private final JTextArea translationText = new ShadowTextArea("");
    private final JPanel contentPanel;
    private boolean locked;
    private Point dragStartOnScreen;
    private Point dragStartWindowLocation;
    private String displayedSourceText = "等待识别结果...";
    private String displayedTranslationText = "";
    private int contentHeight = -1;
    private int lineGap = 4;

    // ---- animation state ----
    private float animProgress = 1f;
    private Timer animTimer;
    private boolean animating;

    public FloatingSubtitleWindow() {
        setAlwaysOnTop(true);
        setBackground(new Color(0, 0, 0, 0));
        this.contentPanel = buildContent();
        setContentPane(contentPanel);
        resizeToContent();
        pack();
        moveToBottomCenter();
        applyInteractionState();
    }

    public void updateSubtitle(String sourceText, String translatedText) {
        String latestSource = FloatingSubtitleText.compact(sourceText, MAX_SOURCE_CHARACTERS);
        String latestTranslation = FloatingSubtitleText.compact(translatedText, MAX_TRANSLATION_CHARACTERS);
        String nextSourceText = latestSource.isBlank() ? "等待识别结果..." : latestSource;
        if (displayedSourceText.equals(nextSourceText) && displayedTranslationText.equals(latestTranslation)) {
            return;
        }
        displayedSourceText = nextSourceText;
        displayedTranslationText = latestTranslation;
        this.sourceText.setText(displayedSourceText);
        this.translationText.setText(displayedTranslationText);
        resizeToContent();
        startAnimating();
    }

    public void reset() {
        updateSubtitle("等待识别结果...", "");
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
        dragStartOnScreen = null;
        dragStartWindowLocation = null;
        applyInteractionState();
    }

    public boolean isLocked() {
        return locked;
    }

    public void setSourceTextColor(Color color) {
        if (color != null) {
            sourceText.setForeground(color);
        }
    }

    public void setTranslationTextColor(Color color) {
        if (color != null) {
            translationText.setForeground(color);
        }
    }

    public void applyDisplaySettings(Color sourceColor, Color translationColor,
                                      int sourceFontSize, int translationFontSize) {
        if (sourceColor != null) {
            sourceText.setForeground(sourceColor);
        }
        if (translationColor != null) {
            translationText.setForeground(translationColor);
        }
        sourceText.setFont(new Font(Font.SANS_SERIF, Font.BOLD, sourceFontSize));
        translationText.setFont(new Font(Font.SANS_SERIF, Font.BOLD, translationFontSize));
        contentHeight = -1;
        resizeToContent();
        moveToBottomCenter();
        repaint();
    }

    public void setLineGap(int gap) {
        this.lineGap = Math.max(0, Math.min(gap, 16));
        contentHeight = -1;
        resizeToContent();
        repaint();
    }

    public Point getSavedPosition() {
        return getLocation();
    }

    public void restorePosition(int x, int y) {
        if (x >= 0 && y >= 0) {
            Rectangle screen = currentScreenBounds();
            setLocation(
                    Math.min(Math.max(x, screen.x), screen.x + Math.max(0, screen.width - getWidth())),
                    Math.min(Math.max(y, screen.y), screen.y + Math.max(0, screen.height - getHeight()))
            );
        } else {
            moveToBottomCenter();
        }
    }

    // ---- internal ----

    private JPanel buildContent() {
        JPanel panel = new RoundedBackgroundPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(16, 32, 16, 32));
        DragHandler dragHandler = new DragHandler();
        bindDragHandler(panel, dragHandler);
        configureTextArea(sourceText, 26, new Color(255, 255, 255));
        configureTextArea(translationText, 28, new Color(255, 230, 150));
        bindDragHandler(sourceText, dragHandler);
        bindDragHandler(translationText, dragHandler);
        panel.add(sourceText);
        panel.add(Box.createVerticalStrut(lineGap));
        panel.add(translationText);
        return panel;
    }

    private static void bindDragHandler(JComponent component, DragHandler dragHandler) {
        component.addMouseListener(dragHandler);
        component.addMouseMotionListener(dragHandler);
    }

    private void moveToBottomCenter() {
        Rectangle screen = currentScreenBounds();
        int x = screen.x + Math.max(0, (screen.width - WINDOW_WIDTH) / 2);
        int y = screen.y + Math.max(0, screen.height - getHeight() - SCREEN_BOTTOM_MARGIN);
        setLocation(x, y);
    }

    private void resizeToContent() {
        int textWidth = WINDOW_WIDTH - HORIZONTAL_PADDING;
        int sourceHeight = wrappedTextHeight(sourceText, displayedSourceText, textWidth, SOURCE_MAX_LINES);
        int translationHeight = wrappedTextHeight(translationText, displayedTranslationText, textWidth, TRANSLATION_MAX_LINES);
        applyStableTextSize(sourceText, textWidth, sourceHeight);
        applyStableTextSize(translationText, textWidth, translationHeight);
        int windowHeight = Math.max(MIN_WINDOW_HEIGHT, sourceHeight + translationHeight + VERTICAL_PADDING + lineGap);
        if (windowHeight == contentHeight) {
            repaint();
            return;
        }
        contentHeight = windowHeight;
        contentPanel.setPreferredSize(new Dimension(WINDOW_WIDTH, windowHeight));
        pack();
        applyShape();
        keepInsideScreen();
        applyInteractionState();
    }

    private void applyShape() {
        setShape(new RoundRectangle2D.Double(0, 0, Math.max(1, getWidth()), Math.max(1, getHeight()), CORNER_RADIUS, CORNER_RADIUS));
    }

    private static void applyStableTextSize(JTextArea textArea, int width, int height) {
        Dimension size = new Dimension(width, height);
        textArea.setMinimumSize(size);
        textArea.setPreferredSize(size);
        textArea.setMaximumSize(size);
    }

    static int wrappedTextHeight(JTextArea textArea, String text, int width, int maxLines) {
        FontMetrics metrics = textArea.getFontMetrics(textArea.getFont());
        int lineHeight = metrics.getHeight();
        int lines = FloatingSubtitleText.wrappedLineCount(text, metrics, width);
        int visibleLines = Math.max(1, Math.min(lines, maxLines));
        return visibleLines * lineHeight;
    }

    private void keepInsideScreen() {
        Rectangle screen = currentScreenBounds();
        int x = Math.min(Math.max(getX(), screen.x), screen.x + Math.max(0, screen.width - getWidth()));
        int y = Math.min(Math.max(getY(), screen.y), screen.y + Math.max(0, screen.height - getHeight()));
        setLocation(x, y);
    }

    private Rectangle currentScreenBounds() {
        return GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice()
                .getDefaultConfiguration()
                .getBounds();
    }

    private void applyInteractionState() {
        setAlwaysOnTop(true);
        setFocusableWindowState(!locked);
        WindowsWindowHitTestController.apply(this, locked);
    }

    // ---- animation ----

    private void startAnimating() {
        if (animTimer != null) {
            animTimer.stop();
        }
        animProgress = 0f;
        animating = true;
        animTimer = new Timer(16, event -> {
            animProgress += 16f / ANIM_DURATION_MS;
            if (animProgress >= 1f) {
                animProgress = 1f;
                animating = false;
                animTimer.stop();
            }
            repaint();
        });
        animTimer.setRepeats(true);
        animTimer.start();
    }

    // ---- custom components ----

    /** Panel that paints the rounded semi-transparent backdrop and delegates animation to children. */
    private final class RoundedBackgroundPanel extends JPanel {
        private final Map<?, ?> desktopHints;

        RoundedBackgroundPanel() {
            setOpaque(false);
            desktopHints = (Map<?, ?>) Toolkit.getDefaultToolkit()
                    .getDesktopProperty("awt.font.desktophints");
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g.create();
            try {
                applyAlphaForAnimation(g2d);
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (desktopHints != null) {
                    g2d.addRenderingHints(desktopHints);
                }
                g2d.setColor(Theme.FLOAT_BG);
                g2d.fill(new RoundRectangle2D.Double(
                        0, 0, getWidth(), getHeight(), CORNER_RADIUS, CORNER_RADIUS));
            } finally {
                g2d.dispose();
            }
            super.paintComponent(g);
        }

        private void applyAlphaForAnimation(Graphics2D g2d) {
            if (animating && animProgress < 1f) {
                float eased = easeOutCubic(animProgress);
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, eased));
            }
        }

        private static float easeOutCubic(float t) {
            return 1f - (1f - t) * (1f - t) * (1f - t);
        }
    }

    /** JTextArea that renders text with a drop shadow for legibility on any background. */
    private static final class ShadowTextArea extends JTextArea {
        private final Map<?, ?> desktopHints;

        ShadowTextArea(String initialText) {
            super(initialText);
            desktopHints = (Map<?, ?>) Toolkit.getDefaultToolkit()
                    .getDesktopProperty("awt.font.desktophints");
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g.create();
            try {
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                if (desktopHints != null) {
                    g2d.addRenderingHints(desktopHints);
                }
                g2d.setColor(Theme.FLOAT_SHADOW);
                g2d.translate(1, 1);
                super.paintComponent(g2d);
            } finally {
                g2d.dispose();
            }
            g.translate(0, 0);
            super.paintComponent(g);
        }
    }

    // ---- text area configuration ----

    private static void configureTextArea(JTextArea textArea, int size, Color color) {
        textArea.setForeground(color);
        textArea.setFont(new Font(Font.SANS_SERIF, Font.BOLD, size));
        textArea.setOpaque(false);
        textArea.setEditable(false);
        textArea.setFocusable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setAlignmentX(CENTER_ALIGNMENT);
    }

    // ---- drag handler ----

    private final class DragHandler extends MouseAdapter {
        @Override
        public void mousePressed(MouseEvent event) {
            if (locked) {
                return;
            }
            dragStartOnScreen = event.getLocationOnScreen();
            dragStartWindowLocation = getLocation();
        }

        @Override
        public void mouseDragged(MouseEvent event) {
            if (locked) {
                return;
            }
            if (dragStartOnScreen == null || dragStartWindowLocation == null) {
                return;
            }
            Point current = event.getLocationOnScreen();
            int dx = current.x - dragStartOnScreen.x;
            int dy = current.y - dragStartOnScreen.y;
            setLocation(dragStartWindowLocation.x + dx, dragStartWindowLocation.y + dy);
        }
    }
}
