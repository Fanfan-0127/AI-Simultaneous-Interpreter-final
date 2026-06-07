package com.fanfan.interpreter.ui;

import com.fanfan.interpreter.config.UserSettings;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;

public final class SettingsDialog extends JDialog {

    private final UserSettings settings;
    private final Runnable onSaved;

    // API tab
    private final JPasswordField apiKeyField;
    private final JCheckBox showApiKeyCheckbox = new JCheckBox("显示");
    private final JTextField realtimeUrlField;
    private final JTextField asrModelField;
    private final JTextField mtModelField;

    // Audio & Translation tab
    private final JTextField asrLanguageField;
    private final JSpinner asrSampleRateSpinner;
    private final JSpinner asrVadThresholdSpinner;
    private final JSpinner asrVadSilenceMsSpinner;
    private final JSpinner asrStabilityDelayMsSpinner;
    private final JSpinner draftDelayMsSpinner;

    // Display tab
    private Color sourceColor;
    private Color translationColor;
    private final JButton sourceColorButton;
    private final JButton translationColorButton;
    private final JLabel sourceColorHex = new JLabel();
    private final JLabel translationColorHex = new JLabel();
    private final JSpinner sourceFontSizeSpinner;
    private final JSpinner translationFontSizeSpinner;
    private final JSpinner lineSpacingSpinner;
    private final JComboBox<String> themeCombo;
    private final JComboBox<String> sourceFontCombo;
    private final JComboBox<String> translationFontCombo;
    private final JSpinner bgOpacitySpinner;
    private final JCheckBox showSourceCheckbox;

    private SettingsDialog(Frame owner, UserSettings settings, Runnable onSaved) {
        super(owner, "偏好设置", true);
        this.settings = settings;
        this.onSaved = onSaved;
        this.sourceColor = parseColor(settings.floatingSourceColor(), Color.WHITE);
        this.translationColor = parseColor(settings.floatingTranslationColor(), new Color(255, 230, 150));

        apiKeyField = new JPasswordField(settings.apiKey(), 36);
        realtimeUrlField = new JTextField(settings.realtimeUrl(), 36);
        asrModelField = new JTextField(settings.asrModel(), 20);
        mtModelField = new JTextField(settings.mtModel(), 20);

        asrLanguageField = new JTextField(settings.asrLanguage(), 8);
        asrSampleRateSpinner = new JSpinner(new SpinnerNumberModel(settings.asrSampleRate(), 8000, 48000, 1000));
        asrVadThresholdSpinner = new JSpinner(new SpinnerNumberModel((double) settings.asrVadThreshold(), 0.0, 1.0, 0.1));
        asrVadSilenceMsSpinner = new JSpinner(new SpinnerNumberModel(settings.asrVadSilenceMs(), 100, 5000, 100));
        asrStabilityDelayMsSpinner = new JSpinner(new SpinnerNumberModel(settings.asrStabilityDelayMs(), 50, 2000, 50));
        draftDelayMsSpinner = new JSpinner(new SpinnerNumberModel((long) settings.draftDelayMs(), 0L, 1000L, 20L));

        sourceColorButton = new JButton();
        translationColorButton = new JButton();
        sourceFontSizeSpinner = new JSpinner(new SpinnerNumberModel(settings.floatingSourceFontSize(), 12, 48, 1));
        translationFontSizeSpinner = new JSpinner(new SpinnerNumberModel(settings.floatingTranslationFontSize(), 12, 48, 1));
        lineSpacingSpinner = new JSpinner(new SpinnerNumberModel(settings.floatingLineSpacing(), 0, 16, 1));
        themeCombo = new JComboBox<>(new String[]{"暗色主题", "亮色主题"});
        themeCombo.setSelectedIndex("light".equals(settings.theme()) ? 1 : 0);

        String[] systemFonts = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getAvailableFontFamilyNames();
        java.util.Arrays.sort(systemFonts);
        sourceFontCombo = new JComboBox<>(systemFonts);
        sourceFontCombo.setSelectedItem(settings.floatingSourceFont());
        translationFontCombo = new JComboBox<>(systemFonts);
        translationFontCombo.setSelectedItem(settings.floatingTranslationFont());

        bgOpacitySpinner = new JSpinner(new SpinnerNumberModel(settings.floatingBgOpacity(), 0, 255, 5));
        showSourceCheckbox = new JCheckBox("显示原文", settings.floatingShowSource());

        buildUi();
    }

    public static void show(Frame owner, UserSettings settings, Runnable onSaved) {
        SettingsDialog dialog = new SettingsDialog(owner, settings, onSaved);
        dialog.setVisible(true);
    }

    private void buildUi() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("API 设置", buildApiPanel());
        tabs.addTab("音频与翻译", buildAudioPanel());
        tabs.addTab("显示", buildDisplayPanel());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        JButton resetButton = new JButton("恢复默认");
        JButton saveButton = new JButton("保存");
        JButton cancelButton = new JButton("取消");
        resetButton.addActionListener(event -> resetDisplayDefaults());
        saveButton.addActionListener(event -> saveAndClose());
        cancelButton.addActionListener(event -> dispose());
        buttonPanel.add(resetButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        content.add(tabs, BorderLayout.CENTER);
        content.add(buttonPanel, BorderLayout.SOUTH);

        setContentPane(content);
        pack();
        setLocationRelativeTo(getOwner());
        setResizable(false);
    }

    private JPanel buildApiPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(6, 4, 6, 4);
        c.weightx = 1;

        addLabel(panel, c, "API Key");
        JPanel apiKeyPanel = new JPanel(new BorderLayout(4, 0));
        apiKeyPanel.add(apiKeyField, BorderLayout.CENTER);
        showApiKeyCheckbox.setSelected(false);
        showApiKeyCheckbox.addActionListener(event -> {
            apiKeyField.setEchoChar(showApiKeyCheckbox.isSelected() ? (char) 0 : '•');
            apiKeyField.repaint();
        });
        apiKeyPanel.add(showApiKeyCheckbox, BorderLayout.EAST);
        addField(panel, c, apiKeyPanel);

        addLabel(panel, c, "WebSocket URL");
        addField(panel, c, realtimeUrlField);

        addLabel(panel, c, "ASR 模型");
        addField(panel, c, asrModelField);

        addLabel(panel, c, "MT 模型");
        addField(panel, c, mtModelField);

        // filler row to push everything to the top
        c.gridy++;
        c.weighty = 1;
        panel.add(new JLabel(), c);

        return panel;
    }

    private JPanel buildAudioPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(6, 4, 6, 4);
        c.weightx = 1;

        addLabel(panel, c, "识别语言");
        addField(panel, c, asrLanguageField);

        addLabel(panel, c, "采样率 (Hz)");
        addField(panel, c, asrSampleRateSpinner);

        addLabel(panel, c, "VAD 阈值");
        addField(panel, c, asrVadThresholdSpinner);

        addLabel(panel, c, "VAD 静音时长 (ms)");
        addField(panel, c, asrVadSilenceMsSpinner);

        addLabel(panel, c, "稳定延迟 (ms)");
        addField(panel, c, asrStabilityDelayMsSpinner);

        addLabel(panel, c, "翻译草稿延迟 (ms)");
        addField(panel, c, draftDelayMsSpinner);

        c.gridy++;
        c.weighty = 1;
        panel.add(new JLabel(), c);

        return panel;
    }

    private JPanel buildDisplayPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(6, 4, 6, 4);
        c.weightx = 1;

        addLabel(panel, c, "主题");
        addField(panel, c, themeCombo);

        addLabel(panel, c, "原文颜色");
        configureColorButton(sourceColorButton, sourceColor);
        updateColorHex(sourceColorHex, sourceColor);
        sourceColorButton.addActionListener(event -> {
            Color chosen = JColorChooser.showDialog(this, "选择原文颜色", sourceColor);
            if (chosen != null) {
                sourceColor = chosen;
                configureColorButton(sourceColorButton, sourceColor);
                updateColorHex(sourceColorHex, sourceColor);
            }
        });
        JPanel sourceColorRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        sourceColorRow.setOpaque(false);
        sourceColorRow.add(sourceColorButton);
        sourceColorRow.add(sourceColorHex);
        addField(panel, c, sourceColorRow);

        addLabel(panel, c, "译文颜色");
        configureColorButton(translationColorButton, translationColor);
        updateColorHex(translationColorHex, translationColor);
        translationColorButton.addActionListener(event -> {
            Color chosen = JColorChooser.showDialog(this, "选择译文颜色", translationColor);
            if (chosen != null) {
                translationColor = chosen;
                configureColorButton(translationColorButton, translationColor);
                updateColorHex(translationColorHex, translationColor);
            }
        });
        JPanel transColorRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        transColorRow.setOpaque(false);
        transColorRow.add(translationColorButton);
        transColorRow.add(translationColorHex);
        addField(panel, c, transColorRow);

        addLabel(panel, c, "原文字号 (pt)");
        addField(panel, c, sourceFontSizeSpinner);

        addLabel(panel, c, "译文字号 (pt)");
        addField(panel, c, translationFontSizeSpinner);

        addLabel(panel, c, "行间距 (px)");
        addField(panel, c, lineSpacingSpinner);

        addLabel(panel, c, "原文字体");
        addField(panel, c, sourceFontCombo);

        addLabel(panel, c, "译文字体");
        addField(panel, c, translationFontCombo);

        addLabel(panel, c, "背景不透明度");
        addField(panel, c, bgOpacitySpinner);

        addLabel(panel, c, "悬浮窗选项");
        addField(panel, c, showSourceCheckbox);

        c.gridy++;
        c.weighty = 1;
        panel.add(new JLabel(), c);

        return panel;
    }

    private static void updateColorHex(JLabel label, Color color) {
        label.setText(colorToHex(color));
        label.setFont(new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.PLAIN, 11));
    }

    private void saveAndClose() {
        settings.setApiKey(new String(apiKeyField.getPassword()));
        settings.setRealtimeUrl(realtimeUrlField.getText());
        settings.setAsrModel(asrModelField.getText());
        settings.setMtModel(mtModelField.getText());
        settings.setAsrLanguage(asrLanguageField.getText());
        settings.setAsrSampleRate(((Number) asrSampleRateSpinner.getValue()).intValue());
        settings.setAsrVadThreshold(((Number) asrVadThresholdSpinner.getValue()).floatValue());
        settings.setAsrVadSilenceMs(((Number) asrVadSilenceMsSpinner.getValue()).intValue());
        settings.setAsrStabilityDelayMs(((Number) asrStabilityDelayMsSpinner.getValue()).intValue());
        settings.setDraftDelayMs(((Number) draftDelayMsSpinner.getValue()).longValue());
        settings.setFloatingSourceColor(colorToHex(sourceColor));
        settings.setFloatingTranslationColor(colorToHex(translationColor));
        settings.setFloatingSourceFontSize(((Number) sourceFontSizeSpinner.getValue()).intValue());
        settings.setFloatingTranslationFontSize(((Number) translationFontSizeSpinner.getValue()).intValue());
        settings.setFloatingLineSpacing(((Number) lineSpacingSpinner.getValue()).intValue());
        settings.setFloatingSourceFont((String) sourceFontCombo.getSelectedItem());
        settings.setFloatingTranslationFont((String) translationFontCombo.getSelectedItem());
        settings.setFloatingBgOpacity(((Number) bgOpacitySpinner.getValue()).intValue());
        settings.setFloatingShowSource(showSourceCheckbox.isSelected());
        settings.setTheme(themeCombo.getSelectedIndex() == 1 ? "light" : "dark");

        try {
            settings.save();
        } catch (IOException exception) {
            // Save failed silently — settings still apply in-memory for this session
        }

        if (onSaved != null) {
            onSaved.run();
        }
        dispose();
    }

    private void resetDisplayDefaults() {
        sourceColor = Color.WHITE;
        translationColor = new Color(255, 230, 150);
        configureColorButton(sourceColorButton, sourceColor);
        updateColorHex(sourceColorHex, sourceColor);
        configureColorButton(translationColorButton, translationColor);
        updateColorHex(translationColorHex, translationColor);
        sourceFontSizeSpinner.setValue(26);
        translationFontSizeSpinner.setValue(28);
        lineSpacingSpinner.setValue(4);
        themeCombo.setSelectedIndex(0);
        sourceFontCombo.setSelectedItem("SansSerif");
        translationFontCombo.setSelectedItem("SansSerif");
        bgOpacitySpinner.setValue(180);
        showSourceCheckbox.setSelected(true);
    }

    private static void addLabel(JPanel panel, GridBagConstraints c, String text) {
        c.gridy++;
        c.gridx = 0;
        c.gridwidth = 1;
        c.weightx = 0;
        c.anchor = GridBagConstraints.LINE_START;
        panel.add(new JLabel(text), c);
    }

    private static void addField(JPanel panel, GridBagConstraints c, java.awt.Component field) {
        c.gridx = 1;
        c.weightx = 1;
        c.anchor = GridBagConstraints.LINE_START;
        panel.add(field, c);
    }

    private static void configureColorButton(JButton button, Color color) {
        button.setBackground(color);
        button.setPreferredSize(new Dimension(36, 36));
        button.setOpaque(true);
        button.setBorderPainted(true);
    }

    static Color parseColor(String hex, Color fallback) {
        if (hex == null || hex.isBlank()) return fallback;
        try {
            return Color.decode(hex.startsWith("#") ? hex : "#" + hex);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    static String colorToHex(Color color) {
        return String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
    }
}
