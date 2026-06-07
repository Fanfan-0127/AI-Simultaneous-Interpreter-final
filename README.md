# AI 同声传译助手

> 话音未落，字幕已出 —— Windows 桌面实时同声传译工具

基于阿里云 DashScope Qwen ASR + Qwen-MT，实时抓取系统音频，流式识别并翻译为双语字幕，以透明悬浮窗叠加在任意窗口之上。面向国际会议、技术分享、在线课程等场景，主打**极低延迟 + 多语言覆盖 + 专业字幕体验**。

---
## demo视频链接:
【七牛云XEngineer暑期实训营-批次三-题目二-AI同声传译助手】https://www.bilibili.com/video/BV1YSEt6EE22?vd_source=62572507bea08a2f88404d5634fa0e1f


## 亮点

### ⚡ 极致低延迟（~300-800ms 端到端）

从扬声器声波到屏幕字幕，全链路优化：

| 优化点 | 详情 |
|--------|------|
| PCM 音频块 | 50ms 微块推送（行业常见 100ms），减少缓冲等待 |
| 稳定性窗口 | 300ms 自适应去抖，平衡延迟与准确率 |
| WebSocket 预热 | 启动时预连接 ASR，首次监听零等待 |
| LRU 翻译缓存 | 200 条缓存避免重复 API 调用 |
| 双通道翻译 | Draft 快速出稿（180ms 延迟）+ Correction 精准修正 |

### 🌐 多语言覆盖

| 能力 | 规格 |
|------|------|
| 语音识别 | **27 种**源语言（中日英韩德法西葡俄阿…） |
| 翻译目标 | **92 种**目标语言（Qwen-MT 全量支持） |
| 源语言检测 | MT 自动检测（`auto`），无需手动指定 |
| 术语纠错 | 英文源语言自动术语修正（OpenAI、Kubernetes 等） |

GUI 设置界面一键切换，保存后自动重启应用使配置生效。

### 🖥️ 专业字幕悬浮窗

- **透明置顶** — 悬浮于任意窗口之上，鼠标穿透不干扰操作
- **双语同步** — 原文 + 译文实时刷新，草稿热更新 + 终版修正
- **深度可定制** — 原文/译文字体、字号、颜色、行间距、背景不透明度均可独立调节
- **暗色/亮色双主题** — 精心调色，非简单反色
- **拖动/锁定** — 自由拖动位置，一键锁定防误触，位置记忆跨会话持久化
- **原文显隐** — 可隐藏原文仅显示译文，背景自适应缩小

### 🎛️ 完整工作台

主窗口提供全流程控制与可视化：

- **音频源选择** — WASAPI Loopback 回采系统音频 + Java Sound 兜底，实时电平指示
- **字幕历史** — 完整记录所有字幕条目，原文/译文对照表格
- **修正追溯** — 翻译修正前后对比（可折叠面板），展示系统自我修正能力
- **延迟监控** — 实时显示端到端延迟
- **术语提取** — 自动检测高频术语，注入 ASR 纠错白名单
- **字幕导出** — UTF-8 txt，含时间戳 + 状态标签（DRAFT / FINAL / CORRECTED）

### 🛡️ 演示级稳定性

多层防护：

- WASAPI 设备占用诊断 → 中文引导用户关闭冲突程序
- 翻译失败降级展示 → 显示原文 + 错误提示，不崩溃不空白
- API Key 环境变量管理 → 零硬编码密钥

---

## 快速开始

### 前置要求

- Windows 10/11
- Java 21+
- [DashScope API Key](https://help.aliyun.com/zh/model-studio/get-api-key)

### 从源码启动

```powershell
# 设置 API Key（必需）
$env:DASHSCOPE_API_KEY="sk-..."

# 可选：自定义语言对（默认 English → 中文）
$env:ASR_LANGUAGE="ja"
$env:TARGET_LANGUAGE="Chinese"

# 启动
mvn exec:java -Dexec.mainClass=com.fanfan.interpreter.App
```

### Windows 打包（jpackage）

生成自带 JRE 的原生 Windows 程序，无需安装 Java：

```powershell
.\package.ps1
```

输出到 `dist/AI-Simultaneous-Interpreter/`，双击 `AI-Simultaneous-Interpreter.exe` 启动。

---

## 配置参考

### 环境变量

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `DASHSCOPE_API_KEY` | — | **必需**。DashScope API Key |
| `DASHSCOPE_REALTIME_URL` | `wss://dashscope.aliyuncs.com/api-ws/v1/realtime` | ASR WebSocket 地址 |
| `DASHSCOPE_REALTIME_MODEL` | `qwen3-asr-flash-realtime` | ASR 模型 |
| `DASHSCOPE_MT_MODEL` | `qwen-mt-flash` | 翻译模型 |
| `ASR_LANGUAGE` | `en` | 音频源语言代码 |
| `TARGET_LANGUAGE` | `Chinese` | 翻译目标语言（MT API 名称） |
| `ASR_SAMPLE_RATE` | `16000` | 音频采样率（Hz） |
| `ASR_VAD_THRESHOLD` | `0.0` | VAD 检测阈值 |
| `ASR_VAD_SILENCE_MS` | `800` | VAD 静音断句时长（ms） |
| `ASR_STABILITY_DELAY_MS` | `300` | 字幕稳定延迟（ms） |

环境变量优先级高于 GUI 设置（同名变量以环境变量为准）。通过 GUI 修改设置后应用会自动重启使配置生效。

---

## 架构

```
┌─ 音频采集 ─────────────────────────────────────────┐
│  WASAPI Loopback (JNA)  ·  Java Sound (兜底)       │
│  16kHz mono PCM  ·  50ms 微块推送  ·  电平监控      │
└──────────────────┬────────────────────────────────┘
                   ▼
┌─ 语音识别 ─────────────────────────────────────────┐
│  Qwen Realtime ASR  ·  WebSocket 流式  ·  预热连接  │
│  27 种语言  ·  300ms 稳定性窗口  ·  术语自动纠错      │
└──────────────────┬─────────────────────────────────┘
                   ▼
┌─ 翻译引擎 ─────────────────────────────────────────┐
│  Qwen-MT (sourceLang: auto)  ·  92 种目标语言      │
│  LRU 缓存 (200条)  ·  Draft + Correction 双通      │
└──────────────────┬────────────────────────────────┘
                   ▼
┌─ 字幕展示 ─────────────────────────────────────────┐
│  透明置顶悬浮窗  ·  双语同步刷新  ·  拖动/锁定       │
│  颜色/字体/字号/间距/不透明度 全可配  ·  暗亮双主题   │
└────────────────────────────────────────────────────┘
```

---

## 技术栈

Java 21 · Swing · FlatLaf · Maven · DashScope Qwen ASR (WebSocket) · Qwen-MT (HTTP) · JNA WASAPI · Gson · JUnit 5

---

## 许可证

MIT
