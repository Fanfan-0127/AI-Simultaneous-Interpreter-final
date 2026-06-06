package com.fanfan.interpreter.ui;

import com.fanfan.interpreter.asr.AsrClient;
import com.fanfan.interpreter.asr.QwenRealtimeAsrClient;
import com.fanfan.interpreter.asr.StableTranscriptScheduler;
import com.fanfan.interpreter.asr.TranscriptCorrector;
import com.fanfan.interpreter.audio.AudioCaptureService;
import com.fanfan.interpreter.audio.AudioSource;
import com.fanfan.interpreter.config.AppConfig;
import com.fanfan.interpreter.export.SubtitleTxtExporter;
import com.fanfan.interpreter.model.SubtitleEntry;
import com.fanfan.interpreter.model.SubtitleRevision;
import com.fanfan.interpreter.model.SubtitleRevisionType;
import com.fanfan.interpreter.model.SubtitleStore;
import com.fanfan.interpreter.model.SubtitleStore.SubtitleUpdate;
import com.fanfan.interpreter.translation.QwenMtTranslator;
import com.fanfan.interpreter.translation.TranslationScheduler;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainWindow extends JFrame {
    private final AppConfig config = AppConfig.fromEnvironment();
    private final SubtitleStore subtitleStore = new SubtitleStore();
    private final AudioCaptureService audioCaptureService = new AudioCaptureService();
    private final TranslationScheduler translationScheduler = new TranslationScheduler(new QwenMtTranslator(config));
    private final StableTranscriptScheduler stableTranscriptScheduler = new StableTranscriptScheduler(config.asrStabilityDelayMs());
    private final ExecutorService controlExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "session-control");
        thread.setDaemon(true);
        return thread;
    });

    private final JComboBox<AudioSource> sourceCombo = new JComboBox<>();
    private final JButton refreshButton = new JButton("刷新音频源");
    private final JButton startButton = new JButton("开始");
    private final JButton stopButton = new JButton("结束");
    private final JButton exportButton = new JButton("保存字幕");
    private final JLabel statusLabel = new JLabel("未监听");
    private final SubtitleTableModel subtitleTableModel = new SubtitleTableModel();
    private final CorrectionTableModel correctionTableModel = new CorrectionTableModel();
    private final JTextArea liveSubtitle = new JTextArea();
    private final FloatingSubtitleWindow floatingSubtitleWindow = new FloatingSubtitleWindow();
    private volatile AsrClient asrClient;
    private String previewSourceText = "等待识别结果...";
    private String previewTranslatedText = "";

    public MainWindow() {
        super("AI 同声传译助手");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(900, 620));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(12, 12));
        add(buildToolbar(), BorderLayout.NORTH);
        add(buildContent(), BorderLayout.CENTER);
        refreshSources();
        stopButton.setEnabled(false);
        bindActions();
        pack();
        floatingSubtitleWindow.setVisible(true);
    }

    private JPanel buildToolbar() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        sourceCombo.setPreferredSize(new Dimension(420, 28));
        panel.add(new JLabel("音频源"));
        panel.add(sourceCombo);
        panel.add(refreshButton);
        panel.add(startButton);
        panel.add(stopButton);
        panel.add(exportButton);
        panel.add(statusLabel);
        return panel;
    }

    private JSplitPane buildContent() {
        JTable table = new JTable(subtitleTableModel);
        table.setRowHeight(30);
        table.getColumnModel().getColumn(0).setPreferredWidth(180);
        table.getColumnModel().getColumn(1).setPreferredWidth(520);
        table.getColumnModel().getColumn(2).setPreferredWidth(240);

        JTable correctionTable = new JTable(correctionTableModel);
        correctionTable.setRowHeight(30);
        correctionTable.getColumnModel().getColumn(0).setPreferredWidth(180);
        correctionTable.getColumnModel().getColumn(1).setPreferredWidth(120);
        correctionTable.getColumnModel().getColumn(2).setPreferredWidth(360);
        correctionTable.getColumnModel().getColumn(3).setPreferredWidth(360);

        liveSubtitle.setEditable(false);
        liveSubtitle.setLineWrap(true);
        liveSubtitle.setWrapStyleWord(true);
        liveSubtitle.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        liveSubtitle.setText("等待识别结果...");

        JSplitPane recordsPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(table), new JScrollPane(correctionTable));
        recordsPane.setResizeWeight(0.68);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                recordsPane, new JScrollPane(liveSubtitle));
        splitPane.setResizeWeight(0.78);
        return splitPane;
    }

    private void bindActions() {
        refreshButton.addActionListener(event -> refreshSources());
        startButton.addActionListener(event -> startSession());
        stopButton.addActionListener(event -> stopSession());
        exportButton.addActionListener(event -> exportSubtitles());
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent event) {
                stopSession();
                audioCaptureService.close();
                translationScheduler.close();
                stableTranscriptScheduler.close();
                floatingSubtitleWindow.dispose();
                controlExecutor.shutdownNow();
            }
        });
    }

    private void refreshSources() {
        List<AudioSource> sources = AudioCaptureService.listSources();
        sourceCombo.setModel(new DefaultComboBoxModel<>(sources.toArray(AudioSource[]::new)));
        if (sources.isEmpty()) statusLabel.setText("未找到 16k PCM 输入音频源");
    }

    private void startSession() {
        AudioSource selectedSource = (AudioSource) sourceCombo.getSelectedItem();
        if (selectedSource == null) {
            JOptionPane.showMessageDialog(this, "请先选择音频源。", "无法开始", JOptionPane.WARNING_MESSAGE);
            return;
        }
        startButton.setEnabled(false);
        refreshButton.setEnabled(false);
        statusLabel.setText("正在连接 Qwen ASR...");
        subtitleStore.clear();
        stableTranscriptScheduler.clear();
        subtitleTableModel.setEntries(List.of());
        correctionTableModel.setRevisions(List.of());
        previewSourceText = "等待识别结果...";
        previewTranslatedText = "";
        liveSubtitle.setText(previewText(previewSourceText, previewTranslatedText));
        floatingSubtitleWindow.reset();
        controlExecutor.submit(() -> {
            try {
                AsrClient client = new QwenRealtimeAsrClient(config);
                client.start(this::onTranscript);
                asrClient = client;
                audioCaptureService.start(selectedSource, client::appendPcm);
                SwingUtilities.invokeLater(() -> {
                    stopButton.setEnabled(true);
                    statusLabel.setText("正在监听 / 正在识别 / 正在翻译");
                });
            } catch (Exception exception) {
                SwingUtilities.invokeLater(() -> {
                    startButton.setEnabled(true);
                    refreshButton.setEnabled(true);
                    statusLabel.setText("启动失败");
                    JOptionPane.showMessageDialog(this, exception.getMessage(), "启动失败", JOptionPane.ERROR_MESSAGE);
                });
            }
        });
    }

    private void stopSession() {
        startButton.setEnabled(true);
        refreshButton.setEnabled(true);
        stopButton.setEnabled(false);
        statusLabel.setText("正在结束...");
        audioCaptureService.stop();
        AsrClient client = asrClient;
        asrClient = null;
        if (client == null) { statusLabel.setText("未监听"); return; }
        controlExecutor.submit(() -> {
            try { client.finish(); }
            catch (Exception ignored) {}
            finally {
                client.close();
                SwingUtilities.invokeLater(() -> statusLabel.setText("已结束"));
            }
        });
    }

    private void exportSubtitles() {
        List<SubtitleEntry> entries = subtitleStore.snapshot();
        if (entries.isEmpty()) {
            JOptionPane.showMessageDialog(this, "当前没有可保存的字幕。", "保存字幕", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("保存字幕");
        chooser.setSelectedFile(new File(defaultExportFileName()));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            SubtitleTxtExporter.export(entries, chooser.getSelectedFile().toPath());
            statusLabel.setText("字幕已保存");
        } catch (IOException exception) {
            JOptionPane.showMessageDialog(this, exception.getMessage(), "保存失败", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static String defaultExportFileName() {
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        return "subtitles-" + ts + ".txt";
    }

    private void onTranscript(String text, boolean finalResult) {
        stableTranscriptScheduler.accept(text, finalResult, this::onStableTranscript);
    }

    private void onStableTranscript(String text, boolean finalResult) {
        String correctedText = TranscriptCorrector.correct(text);
        SwingUtilities.invokeLater(() -> {
            SubtitleUpdate update = subtitleStore.applyTranscript(correctedText, finalResult);
            subtitleTableModel.setEntries(update.entries());
            correctionTableModel.setRevisions(update.revisions());
            updateLiveSubtitle(update.entry());
            translationScheduler.translate(update.entry(), finalResult, this::onTranslation, this::onTranslationError);
        });
    }

    private void onTranslation(TranslationScheduler.TranslationResult result) {
        SwingUtilities.invokeLater(() -> {
            List<SubtitleEntry> entries = subtitleStore.applyTranslation(
                    result.entryId(), result.sourceVersion(), result.translatedText());
            subtitleTableModel.setEntries(entries);
            correctionTableModel.setRevisions(subtitleStore.revisionSnapshot());
            updateLiveSubtitle(entries.isEmpty() ? null : entries.getLast());
        });
    }

    private void onTranslationError(Exception exception) {
        SwingUtilities.invokeLater(() -> statusLabel.setText("翻译失败: " + exception.getMessage()));
    }

    private void updateLiveSubtitle(SubtitleEntry entry) {
        if (entry == null) return;
        String translated = entry.translatedText().isBlank() ? "翻译中..." : entry.translatedText();
        previewSourceText = entry.sourceText();
        previewTranslatedText = translated;
        liveSubtitle.setText(previewText(previewSourceText, previewTranslatedText));
        floatingSubtitleWindow.updateSubtitle(previewSourceText, previewTranslatedText);
    }

    static String previewText(String source, String translation) {
        String s = source == null ? "" : source;
        String t = translation == null ? "" : translation;
        return t.isBlank() ? s : s + System.lineSeparator() + t;
    }

    static final class SubtitleTableModel extends AbstractTableModel {
        private final String[] columns = {"时间", "英文原文", "中文译文"};
        private List<SubtitleEntry> entries = List.of();

        void setEntries(List<SubtitleEntry> entries) {
            this.entries = List.copyOf(entries);
            fireTableDataChanged();
        }

        @Override public int getRowCount() { return entries.size(); }
        @Override public int getColumnCount() { return columns.length; }
        @Override public String getColumnName(int column) { return columns[column]; }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            SubtitleEntry entry = entries.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> entry.createdAt().toString();
                case 1 -> entry.sourceText();
                case 2 -> entry.translatedText();
                default -> "";
            };
        }
    }

    static final class CorrectionTableModel extends AbstractTableModel {
        private static final int MAX_ROWS = 30;
        private final String[] columns = {"时间", "类型", "修正前", "修正后"};
        private List<SubtitleRevision> revisions = List.of();

        void setRevisions(List<SubtitleRevision> revisions) {
            List<SubtitleRevision> filtered = revisions.stream()
                    .filter(CorrectionTableModel::isCorrection).toList();
            int fromIndex = Math.max(0, filtered.size() - MAX_ROWS);
            this.revisions = filtered.subList(fromIndex, filtered.size()).reversed();
            fireTableDataChanged();
        }

        @Override public int getRowCount() { return revisions.size(); }
        @Override public int getColumnCount() { return columns.length; }
        @Override public String getColumnName(int column) { return columns[column]; }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            SubtitleRevision revision = revisions.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> revision.createdAt().toString();
                case 1 -> labelFor(revision.type());
                case 2 -> beforeText(revision);
                case 3 -> afterText(revision);
                default -> "";
            };
        }

        private static boolean isCorrection(SubtitleRevision r) { return r.type() == SubtitleRevisionType.MT_CORRECTION; }
        private static String beforeText(SubtitleRevision r) { return r.oldTranslatedText(); }
        private static String afterText(SubtitleRevision r) { return r.newTranslatedText(); }

        private static String labelFor(SubtitleRevisionType type) {
            return switch (type) {
                case ASR_UPDATE -> "识别修正";
                case MT_DRAFT -> "翻译初稿";
                case MT_CORRECTION -> "翻译修正";
            };
        }
    }
}
