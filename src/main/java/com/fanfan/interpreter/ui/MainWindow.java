package com.fanfan.interpreter.ui;

import com.fanfan.interpreter.audio.AudioSource;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class MainWindow extends JFrame {
    private static final AudioFormat PCM_16K_MONO = new AudioFormat(16000.0f, 16, 1, true, false);

    private final JComboBox<AudioSource> sourceCombo = new JComboBox<>();
    private final JButton refreshButton = new JButton("刷新音频源");
    private final JButton startButton = new JButton("开始");
    private final JButton stopButton = new JButton("结束");
    private final JLabel statusLabel = new JLabel("未监听");

    public MainWindow() {
        super("AI 同声传译助手");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(600, 200));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(12, 12));
        add(buildToolbar(), BorderLayout.NORTH);
        refreshSources();
        stopButton.setEnabled(false);
        bindActions();
        pack();
    }

    private JPanel buildToolbar() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        sourceCombo.setPreferredSize(new Dimension(420, 28));
        panel.add(new JLabel("音频源"));
        panel.add(sourceCombo);
        panel.add(refreshButton);
        panel.add(startButton);
        panel.add(stopButton);
        panel.add(statusLabel);
        return panel;
    }

    private void bindActions() {
        refreshButton.addActionListener(event -> refreshSources());
        startButton.addActionListener(event -> {
            AudioSource selectedSource = (AudioSource) sourceCombo.getSelectedItem();
            if (selectedSource == null) {
                JOptionPane.showMessageDialog(this, "请先选择音频源。", "无法开始", JOptionPane.WARNING_MESSAGE);
                return;
            }
            startButton.setEnabled(false);
            refreshButton.setEnabled(false);
            stopButton.setEnabled(true);
            statusLabel.setText("已开始（功能待实现）");
        });
        stopButton.addActionListener(event -> {
            startButton.setEnabled(true);
            refreshButton.setEnabled(true);
            stopButton.setEnabled(false);
            statusLabel.setText("未监听");
        });
    }

    private void refreshSources() {
        List<AudioSource> sources = listJavaSoundSources();
        sourceCombo.setModel(new DefaultComboBoxModel<>(sources.toArray(AudioSource[]::new)));
        if (sources.isEmpty()) {
            statusLabel.setText("未找到 16k PCM 输入音频源");
        }
    }

    static List<AudioSource> listJavaSoundSources() {
        List<AudioSource> sources = new ArrayList<>();
        DataLine.Info targetInfo = new DataLine.Info(TargetDataLine.class, PCM_16K_MONO);
        for (Mixer.Info mixerInfo : AudioSystem.getMixerInfo()) {
            Mixer mixer = AudioSystem.getMixer(mixerInfo);
            if (mixer.isLineSupported(targetInfo)) {
                String label = mixerInfo.getName() + " - " + mixerInfo.getDescription();
                sources.add(new AudioSource(label, mixerInfo, isLoopbackCandidate(label)));
            }
        }
        sources.sort(Comparator.comparing(AudioSource::loopbackCandidate).reversed()
                .thenComparing(AudioSource::label));
        return sources;
    }

    private static boolean isLoopbackCandidate(String label) {
        String normalized = label.toLowerCase();
        return normalized.contains("stereo mix")
                || normalized.contains("what u hear")
                || normalized.contains("what you hear")
                || normalized.contains("loopback")
                || normalized.contains("wasapi")
                || normalized.contains("speaker")
                || normalized.contains("speakers")
                || normalized.contains("扬声器")
                || normalized.contains("立体声混音")
                || normalized.contains("回放")
                || normalized.contains("回采");
    }
}
