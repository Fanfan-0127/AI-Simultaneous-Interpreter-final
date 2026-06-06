package com.fanfan.interpreter.config;

import javax.swing.JOptionPane;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public final class ConfigValidator {
    private static final int CONNECTION_TIMEOUT_MS = 3000;
    private static final String DASHSCOPE_ENDPOINT = "https://dashscope.aliyuncs.com";

    private ConfigValidator() {
    }

    public static ValidationResult validate(AppConfig config) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (!config.hasApiKey()) {
            errors.add("未检测到 DASHSCOPE_API_KEY 环境变量");
        } else if (config.apiKey().length() < 10) {
            errors.add("DASHSCOPE_API_KEY 格式不正确（长度过短）");
        }

        try {
            testNetworkConnection();
        } catch (Exception e) {
            warnings.add("网络连接测试失败：" + e.getMessage());
            warnings.add("请确保可以访问 dashscope.aliyuncs.com");
        }

        if (config.asrSampleRate() != 16000) {
            warnings.add("ASR 采样率为 " + config.asrSampleRate() + " Hz，推荐使用 16000 Hz");
        }

        return new ValidationResult(errors, warnings);
    }

    private static void testNetworkConnection() throws Exception {
        URL url = new URL(DASHSCOPE_ENDPOINT);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(CONNECTION_TIMEOUT_MS);
        connection.setReadTimeout(CONNECTION_TIMEOUT_MS);
        connection.setRequestMethod("HEAD");

        try {
            int responseCode = connection.getResponseCode();
            if (responseCode >= 400) {
                throw new Exception("HTTP " + responseCode);
            }
        } finally {
            connection.disconnect();
        }
    }

    public static void showValidationResult(ValidationResult result) {
        if (result.errors().isEmpty() && result.warnings().isEmpty()) {
            return;
        }

        StringBuilder message = new StringBuilder("<html><body style='font-family:微软雅黑;'>");

        if (!result.errors().isEmpty()) {
            message.append("<h3 style='color:red;'>❌ 错误（必须修复）</h3><ul>");
            for (String error : result.errors()) {
                message.append("<li>").append(error).append("</li>");
            }
            message.append("</ul>");
        }

        if (!result.warnings().isEmpty()) {
            message.append("<h3 style='color:orange;'>⚠️ 警告（建议处理）</h3><ul>");
            for (String warning : result.warnings()) {
                message.append("<li>").append(warning).append("</li>");
            }
            message.append("</ul>");
        }

        message.append("<p>是否继续启动？</p></body></html>");

        int choice = JOptionPane.showConfirmDialog(
                null,
                message.toString(),
                "配置检查",
                JOptionPane.YES_NO_OPTION,
                result.errors().isEmpty() ? JOptionPane.WARNING_MESSAGE : JOptionPane.ERROR_MESSAGE
        );

        if (choice != JOptionPane.YES_OPTION) {
            System.exit(0);
        }
    }

    public record ValidationResult(List<String> errors, List<String> warnings) {
    }
}
