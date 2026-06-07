package com.fanfan.interpreter.ui;

import com.fanfan.interpreter.asr.AsrClient;
import com.fanfan.interpreter.asr.QwenRealtimeAsrClient;
import com.fanfan.interpreter.asr.SentenceStabilityDetector;
import com.fanfan.interpreter.asr.StableTranscriptScheduler;
import com.fanfan.interpreter.asr.TranscriptCorrector;
import com.fanfan.interpreter.audio.AudioCaptureService;
import com.fanfan.interpreter.audio.AudioLevel;
import com.fanfan.interpreter.audio.AudioSource;
import com.fanfan.interpreter.config.AppConfig;
import com.fanfan.interpreter.config.ConfigValidator;
import com.fanfan.interpreter.config.UserSettings;
import com.fanfan.interpreter.export.SubtitleTxtExporter;
import com.fanfan.interpreter.metrics.LatencyTracker;
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
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainWindow extends JFrame {
    private final UserSettings userSettings = UserSettings.load();
    private final AppConfig config = AppConfig.fromSettings(userSettings);
    private final SubtitleStore subtitleStore = new SubtitleStore();
    private final AudioCaptureService audioCaptureService = new AudioCaptureService();
    private final LatencyTracker latencyTracker = new LatencyTracker();
    private final TranslationScheduler translationScheduler = new TranslationScheduler(new QwenMtTranslator(config), userSettings.draftDelayMs());
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
    private final JButton termsButton = new JButton("查看术语");
    private final JButton lockFloatingButton = new JButton("锁定悬浮窗");
    private final JButton settingsButton = new JButton("⚙ 设置");
    private final JLabel statusLabel = new JLabel("未监听");
    private final JLabel audioStatusLabel = new JLabel("音频未启动");
    private final JLabel latencyLabel = new JLabel("延迟: --");
    private final SubtitleTableModel subtitleTableModel = new SubtitleTableModel();
    private final CorrectionTableModel correctionTableModel = new CorrectionTableModel();
    private final JTextArea liveSubtitle = new JTextArea();
    private final FloatingSubtitleWindow floatingSubtitleWindow = new FloatingSubtitleWindow();
    private Color floatingSourceColor = SettingsDialog.parseColor(userSettings.floatingSourceColor(), Color.WHITE);
    private Color floatingTranslationColor = SettingsDialog.parseColor(userSettings.floatingTranslationColor(), new Color(255, 230, 150));
    private volatile AsrClient asrClient;
    private volatile QwenRealtimeAsrClient preWarmedClient;
    private String previewSourceText = "等待识别结果...";
    private String previewTranslatedText = "";

    public MainWindow() {
        super("AI 同声传译助手");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(900, 620));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(12, 12));

        ConfigValidator.ValidationResult validation = ConfigValidator.validate(config);
        ConfigValidator.showValidationResult(validation);

        preWarmAsrConnection();

        setJMenuBar(buildMenuBar());
        add(buildToolbar(), BorderLayout.NORTH);
        add(buildContent(), BorderLayout.CENTER);
        refreshSources();
        stopButton.setEnabled(false);
        bindActions();
        pack();
        floatingSubtitleWindow.applyDisplaySettings(
                floatingSourceColor, floatingTranslationColor,
                userSettings.floatingSourceFontSize(), userSettings.floatingTranslationFontSize());
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
        panel.add(termsButton);
        panel.add(lockFloatingButton);
        panel.add(settingsButton);
        panel.add(statusLabel);
        panel.add(audioStatusLabel);
        panel.add(latencyLabel);
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
        termsButton.addActionListener(event -> showTermsDialog());
        lockFloatingButton.addActionListener(event -> toggleFloatingWindowLock());
        settingsButton.addActionListener(event -> openSettings());
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent event) {
                stopSession();
                QwenRealtimeAsrClient pw = preWarmedClient;
                preWarmedClient = null;
                if (pw != null) pw.close();
                audioCaptureService.close();
                translationScheduler.close();
                stableTranscriptScheduler.close();
                floatingSubtitleWindow.dispose();
                controlExecutor.shutdownNow();
            }
        });
    }

    private void preWarmAsrConnection() {
        if (!config.hasApiKey()) return;
        controlExecutor.submit(() -> {
            try {
                QwenRealtimeAsrClient client = new QwenRealtimeAsrClient(config);
                client.preConnect();
                preWarmedClient = client;
                SwingUtilities.invokeLater(() -> statusLabel.setText("ASR 连接就绪"));
            } catch (Exception ignored) {
                // Pre-warm failed silently; startSession will create a new connection
            }
        });
    }

    private JMenuBar buildMenuBar() {
        JMenuBar bar = new JMenuBar();

        JMenu fileMenu = new JMenu("文件");
        JMenuItem exportItem = new JMenuItem("保存字幕...");
        exportItem.addActionListener(event -> exportSubtitles());
        fileMenu.add(exportItem);
        fileMenu.addSeparator();
        JMenuItem exitItem = new JMenuItem("退出");
        exitItem.addActionListener(event -> dispose());
        fileMenu.add(exitItem);
        bar.add(fileMenu);

        JMenu settingsMenu = new JMenu("设置");
        JMenuItem prefsItem = new JMenuItem("偏好设置...");
        prefsItem.addActionListener(event -> openSettings());
        settingsMenu.add(prefsItem);
        bar.add(settingsMenu);

        JMenu helpMenu = new JMenu("帮助");
        JMenuItem aboutItem = new JMenuItem("关于...");
        aboutItem.addActionListener(event -> JOptionPane.showMessageDialog(
                this,
                "AI 同声传译助手\n\n" +
                "基于 DashScope Qwen ASR + MT 的实时同声传译工具\n" +
                "适用于英文技术会议、在线课程的实时字幕翻译\n\n" +
                "版本: 0.1.0",
                "关于",
                JOptionPane.INFORMATION_MESSAGE));
        helpMenu.add(aboutItem);
        bar.add(helpMenu);

        return bar;
    }

    private void openSettings() {
        SettingsDialog.show(this, userSettings, () -> {
            floatingSourceColor = SettingsDialog.parseColor(userSettings.floatingSourceColor(), Color.WHITE);
            floatingTranslationColor = SettingsDialog.parseColor(userSettings.floatingTranslationColor(), new Color(255, 230, 150));
            floatingSubtitleWindow.applyDisplaySettings(
                    floatingSourceColor, floatingTranslationColor,
                    userSettings.floatingSourceFontSize(), userSettings.floatingTranslationFontSize());
            updateLiveSubtitle(subtitleStore.snapshot());
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
        audioStatusLabel.setText("音频准备中");
        subtitleStore.clear();
        stableTranscriptScheduler.clear();
        latencyTracker.reset();
        subtitleTableModel.setEntries(List.of());
        correctionTableModel.setRevisions(List.of());
        previewSourceText = "等待识别结果...";
        previewTranslatedText = "";
        liveSubtitle.setText(previewText(previewSourceText, previewTranslatedText));
        floatingSubtitleWindow.reset();
        controlExecutor.submit(() -> {
            try {
                QwenRealtimeAsrClient client = preWarmedClient;
                preWarmedClient = null;
                if (client == null) {
                    client = new QwenRealtimeAsrClient(config);
                }
                client.start(this::onTranscript);
                asrClient = client;
                audioCaptureService.start(selectedSource, client::appendPcm, (chunk, level) -> onAudioLevel(level));
                SwingUtilities.invokeLater(() -> {
                    stopButton.setEnabled(true);
                    statusLabel.setText("正在监听 / 正在识别 / 正在翻译");
                    audioStatusLabel.setText("音频监听中");
                });
            } catch (Exception exception) {
                SwingUtilities.invokeLater(() -> {
                    startButton.setEnabled(true);
                    refreshButton.setEnabled(true);
                    stopButton.setEnabled(false);
                    statusLabel.setText("启动失败");
                    audioStatusLabel.setText("音频未启动");

                    String errorMessage = buildStartupErrorMessage(exception, selectedSource);
                    JOptionPane.showMessageDialog(
                            this,
                            errorMessage,
                            "启动失败",
                            JOptionPane.ERROR_MESSAGE
                    );
                });
            }
        });
    }

    private void stopSession() {
        startButton.setEnabled(true);
        refreshButton.setEnabled(true);
        stopButton.setEnabled(false);
        statusLabel.setText("正在结束...");
        audioStatusLabel.setText("音频未启动");
        latencyLabel.setText("延迟: --");
        audioCaptureService.stop();
        AsrClient client = asrClient;
        asrClient = null;
        if (client == null) { statusLabel.setText("未监听"); return; }
        controlExecutor.submit(() -> {
            try { client.finish(); }
            catch (Exception ignored) {}
            finally {
                client.close();
                preWarmAsrConnection();
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

    private void showTermsDialog() {
        Map<String, String> terms = subtitleStore.extractTerms();
        TranscriptCorrector.addTerms(terms.keySet());
        if (terms.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "未检测到术语。术语提取需要满足以下条件：\n" +
                    "1. 已完成至少一次翻译\n" +
                    "2. 术语在原文中出现至少 2 次\n" +
                    "3. 术语长度在 3-40 个字符之间",
                    "术语结果",
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        StringBuilder content = new StringBuilder();
        content.append("<html><body style='font-family:微软雅黑;font-size:13px;'>");
        content.append("<h3>术语列表（共 ").append(terms.size()).append(" 个）</h3>");
        content.append("<table border='1' cellpadding='6' cellspacing='0'>");
        content.append("<tr style='background-color:#f0f0f0;'>");
        content.append("<th><b>英文术语</b></th>");
        content.append("<th><b>中文释义</b></th>");
        content.append("</tr>");

        for (Map.Entry<String, String> entry : terms.entrySet()) {
            content.append("<tr>");
            content.append("<td>").append(escapeHtml(entry.getKey())).append("</td>");
            content.append("<td>").append(escapeHtml(entry.getValue())).append("</td>");
            content.append("</tr>");
        }

        content.append("</table>");
        content.append("<br/><p style='color:#666;'>提示：术语基于出现频率自动提取，可在导出字幕时手动追加。</p>");
        content.append("</body></html>");

        JOptionPane.showMessageDialog(
                this,
                content.toString(),
                "术语结果",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;");
    }

    private void toggleFloatingWindowLock() {
        boolean nextLocked = !floatingSubtitleWindow.isLocked();
        floatingSubtitleWindow.setLocked(nextLocked);
        lockFloatingButton.setText(nextLocked ? "解锁悬浮窗" : "锁定悬浮窗");
        statusLabel.setText(nextLocked ? "悬浮窗已锁定" : "悬浮窗已解锁");
    }

    private String buildStartupErrorMessage(Exception exception, AudioSource source) {
        String message = exception.getMessage();

        if (message.contains("0x80070057")) {
            return "WASAPI 设备打开失败（错误码：0x80070057）\n\n" +
                   "可能原因：\n" +
                   "1. 所选输出设备当前被其他程序独占\n" +
                   "2. 设备不支持 Loopback 模式\n" +
                   "3. 系统音频服务异常\n\n" +
                   "建议操作：\n" +
                   "• 关闭其他可能使用音频的程序（如音乐播放器、会议软件）\n" +
                   "• 尝试选择另一个音频源\n" +
                   "• 重启电脑后重试";
        }

        if (message.contains("timed out") || message.contains("timeout")) {
            return "WASAPI 启动超时\n\n" +
                   "可能原因：\n" +
                   "1. 输出设备无音频信号\n" +
                   "2. 系统音量被静音或调至最低\n" +
                   "3. 设备驱动响应缓慢\n\n" +
                   "建议操作：\n" +
                   "• 检查系统音量是否正常\n" +
                   "• 播放一段测试音频确认设备工作正常\n" +
                   "• 尝试选择 Java Sound 音频源作为备选方案";
        }

        if (message.contains("CoInitializeEx") || message.contains("COM")) {
            return "Windows COM 初始化失败\n\n" +
                   "可能原因：\n" +
                   "1. 当前线程已初始化 COM 库\n" +
                   "2. 系统资源不足\n\n" +
                   "建议操作：\n" +
                   "• 重启应用程序\n" +
                   "• 重启电脑后重试";
        }

        if (exception instanceof javax.sound.sampled.LineUnavailableException) {
            return "音频设备不可用\n\n" +
                   "设备：" + source.label() + "\n" +
                   "类型：" + source.backend() + "\n\n" +
                   "可能原因：\n" +
                   "1. 设备被其他程序占用\n" +
                   "2. 设备不支持 16kHz PCM 格式\n\n" +
                   "建议操作：\n" +
                   "• 选择其他音频源\n" +
                   "• 优先选择标注为 'WASAPI' 的设备";
        }

        return "启动失败：" + message + "\n\n" +
               "建议操作：\n" +
               "• 检查网络连接是否正常\n" +
               "• 确认 DASHSCOPE_API_KEY 已正确配置\n" +
               "• 查看控制台日志获取详细错误信息";
    }

    private void onTranscript(String text, boolean finalResult) {
        latencyTracker.markAsrReceived();
        stableTranscriptScheduler.accept(text, finalResult, this::onStableTranscript);
    }

    private void onStableTranscript(String text, boolean finalResult) {
        String correctedText = TranscriptCorrector.correct(text);
        boolean stableSentence = SentenceStabilityDetector.isStable(text, finalResult);
        SwingUtilities.invokeLater(() -> {
            SubtitleUpdate update = subtitleStore.applyTranscript(text, finalResult);
            subtitleTableModel.setEntries(update.entries());
            correctionTableModel.setRevisions(update.revisions());
            updateLiveSubtitle(update.entries());
            latencyTracker.markTranslationStarted();
            translationScheduler.translate(update.entry(), finalResult, text, correctedText, stableSentence,
                    this::onTranslation, this::onTranslationError);
        });
    }

    private void onTranslation(TranslationScheduler.TranslationResult result) {
        latencyTracker.markTranslationCompleted();
        SwingUtilities.invokeLater(() -> {
            List<SubtitleEntry> entries = subtitleStore.applyTranslation(
                    result.entryId(), result.sourceVersion(), result.translatedText());
            subtitleTableModel.setEntries(entries);
            correctionTableModel.setRevisions(subtitleStore.revisionSnapshot());
            updateLiveSubtitle(entries);
            updateLatencyDisplay();
        });
    }

    private void onTranslationError(Exception exception) {
        latencyTracker.markTranslationCompleted();
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText("翻译失败: " + exception.getMessage());
            updateLatencyDisplay();
        });
    }

    private void updateLatencyDisplay() {
        SwingUtilities.invokeLater(() -> {
            latencyLabel.setText("延迟: " + latencyTracker.getStatusSummary());
        });
    }

    private void onAudioLevel(AudioLevel level) {
        SwingUtilities.invokeLater(() -> {
            if (asrClient == null) return;
            audioStatusLabel.setText(switch (level.quality()) {
                case SILENT -> "音频静音";
                case LOW -> "音量偏低";
                case CLIPPING -> "爆音风险";
                case NORMAL -> "音频正常";
            });
        });
    }

    private void updateLiveSubtitle(List<SubtitleEntry> entries) {
        if (entries == null || entries.isEmpty()) return;
        SubtitleEntry best = bestEntry(entries);
        String translated = best.translatedText();
        previewSourceText = best.sourceText();
        previewTranslatedText = translated;
        liveSubtitle.setText(previewText(previewSourceText, previewTranslatedText));
        floatingSubtitleWindow.updateSubtitle(previewSourceText, previewTranslatedText);
    }

    static SubtitleEntry bestEntry(List<SubtitleEntry> entries) {
        for (int i = entries.size() - 1; i >= 0; i--) {
            SubtitleEntry entry = entries.get(i);
            if (!entry.translatedText().isBlank()) {
                return entry;
            }
        }
        return entries.getLast();
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
            int oldSize = this.entries.size();
            this.entries = List.copyOf(entries);
            if (this.entries.isEmpty()) {
                fireTableDataChanged();
                return;
            }
            if (oldSize == this.entries.size()) {
                fireTableRowsUpdated(this.entries.size() - 1, this.entries.size() - 1);
                return;
            }
            if (oldSize + 1 == this.entries.size()) {
                fireTableRowsInserted(this.entries.size() - 1, this.entries.size() - 1);
                return;
            }
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
            int oldSize = this.revisions.size();
            this.revisions = filtered.subList(fromIndex, filtered.size()).reversed();
            if (this.revisions.isEmpty() || this.revisions.size() != oldSize) {
                fireTableDataChanged();
                return;
            }
            fireTableRowsUpdated(0, this.revisions.size() - 1);
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
        private static String beforeText(SubtitleRevision r) {
            if (r.type() == SubtitleRevisionType.ASR_UPDATE) return r.oldSourceText();
            return r.oldTranslatedText();
        }
        private static String afterText(SubtitleRevision r) {
            if (r.type() == SubtitleRevisionType.ASR_UPDATE) return r.newSourceText();
            return r.newTranslatedText();
        }

        private static String labelFor(SubtitleRevisionType type) {
            return switch (type) {
                case ASR_UPDATE -> "识别修正";
                case MT_DRAFT -> "翻译初稿";
                case MT_CORRECTION -> "翻译修正";
            };
        }
    }
}
