# AI Simultaneous Interpreter Final

Windows 桌面同声传译 Demo — 实时英语 ASR 字幕与中文字幕。

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
$env:ASR_SAMPLE_RATE="16000"
$env:ASR_VAD_THRESHOLD="0.0"
$env:ASR_VAD_SILENCE_MS="800"
$env:ASR_STABILITY_DELAY_MS="700"
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
