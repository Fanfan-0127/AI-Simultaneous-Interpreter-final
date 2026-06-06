package com.fanfan.interpreter.ui;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JWindow;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public final class FloatingSubtitleWindow extends JWindow {
    private static final int WINDOW_WIDTH = 960;
    private static final int MIN_WINDOW_HEIGHT = 120;
    private static final int HORIZONTAL_PADDING = 64;
    private static final int VERTICAL_PADDING = 32;
    private static final int LINE_GAP = 4;
    private static final int SCREEN_BOTTOM_MARGIN = 72;
    private static final int MAX_SOURCE_CHARACTERS = 140;
    private static final int MAX_TRANSLATION_CHARACTERS = 84;
    private static final int SOURCE_MAX_LINES = 3;
    private static final int TRANSLATION_MAX_LINES = 4;
    private final JTextArea sourceText = new JTextArea("等待识别结果...");
    private final JTextArea translationText = new JTextArea("");
    private final JPanel contentPanel;
    private boolean locked;
    private Point dragStartOnScreen;
    private Point dragStartWindowLocation;
    private String displayedSourceText = "等待识别结果...";
    private String displayedTranslationText = "";
    private int contentHeight = -1;

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

    private JPanel buildContent() {
        JPanel panel = new JPanel();
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
        int windowHeight = Math.max(MIN_WINDOW_HEIGHT, sourceHeight + translationHeight + VERTICAL_PADDING + LINE_GAP);
        if (windowHeight == contentHeight) {
            repaint();
            return;
        }
        contentHeight = windowHeight;
        contentPanel.setPreferredSize(new Dimension(WINDOW_WIDTH, windowHeight));
        pack();
        setShape(fullWindowShape());
        keepInsideScreen();
        applyInteractionState();
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

    private Shape fullWindowShape() {
        return new Rectangle(0, 0, Math.max(1, getWidth()), Math.max(1, getHeight()));
    }

    private void applyInteractionState() {
        setAlwaysOnTop(true);
        setFocusableWindowState(!locked);
        WindowsWindowHitTestController.apply(this, locked);
    }

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
