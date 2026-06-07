# AI Simultaneous Interpreter Final

Windows 桌面同声传译 Demo — 实时语音识别 + 多语言翻译字幕。

支持 **27 种 ASR 源语言** + **92 种 MT 目标语言**，适用于国际会议、技术分享、在线课程等场景。

## 运行

设置 DashScope 凭证：

```powershell
$env:DASHSCOPE_API_KEY="sk-..."
```

可选配置：

```powershell
$env:DASHSCOPE_REALTIME_URL="wss://dashscope.aliyuncs.com/api-ws/v1/realtime"
$env:DASHSCOPE_REALTIME_MODEL="qwen3-asr-flash-realtime"
$env:DASHSCOPE_MT_MODEL="qwen-mt-flash"
$env:ASR_LANGUAGE="en"
$env:TARGET_LANGUAGE="Chinese"
$env:ASR_SAMPLE_RATE="16000"
$env:ASR_VAD_THRESHOLD="0.0"
$env:ASR_VAD_SILENCE_MS="800"
$env:ASR_STABILITY_DELAY_MS="300"
```

启动：

```powershell
mvn exec:java -Dexec.mainClass=com.fanfan.interpreter.App
```

构建可执行 jar：

```powershell
mvn package
java -jar target/ai-simultaneous-interpreter-final-0.1.0-SNAPSHOT.jar
```

## 语言设置

通过 GUI 设置界面（⚙ 设置 → 音频与翻译）或环境变量配置：

| 设置 | 环境变量 | GUI | 说明 |
|------|---------|-----|------|
| 识别语言 | `ASR_LANGUAGE` | 下拉菜单（27 种） | 音频源语言 |
| 翻译目标语言 | `TARGET_LANGUAGE` | 下拉菜单（19 种常用 + 92 种全部） | 字幕显示语言 |

## 功能

- **音频采集** — WASAPI loopback 原生回采 + Java Sound 兜底
- **语音识别** — Qwen Realtime ASR WebSocket 流式，27 种语言
- **翻译** — Qwen-MT 自动检测源语言，92 种目标语言
- **字幕展示** — 透明置顶悬浮窗，双语同步刷新
- **导出** — UTF-8 txt，含时间戳 + 状态标签

## 技术栈

Java 21 · Swing · Maven · DashScope Qwen ASR (WebSocket) · Qwen-MT (HTTP) · JNA WASAPI · FlatLaf · JUnit 5
