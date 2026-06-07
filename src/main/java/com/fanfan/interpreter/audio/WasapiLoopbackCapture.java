package com.fanfan.interpreter.audio;

import com.sun.jna.Function;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.WString;
import com.sun.jna.platform.win32.Guid.CLSID;
import com.sun.jna.platform.win32.Guid.GUID;
import com.sun.jna.platform.win32.Guid.IID;
import com.sun.jna.platform.win32.OleAuto;
import com.sun.jna.platform.win32.Ole32;
import com.sun.jna.platform.win32.Variant;
import com.sun.jna.platform.win32.WTypes;
import com.sun.jna.platform.win32.WinNT.HRESULT;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

import javax.sound.sampled.LineUnavailableException;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

final class WasapiLoopbackCapture implements AutoCloseable {
    private static final CLSID CLSID_MM_DEVICE_ENUMERATOR = new CLSID("{BCDE0395-E52F-467C-8E3D-C4579291692E}");
    private static final IID IID_IMM_DEVICE_ENUMERATOR = new IID("{A95664D2-9614-4F35-A746-DE8DB63617E6}");
    private static final IID IID_IAUDIO_CLIENT = new IID("{1CB9AD4C-DBFA-4c32-B178-C2F568A703B2}");
    private static final IID IID_IAUDIO_CAPTURE_CLIENT = new IID("{C8ADBD64-E71E-48a0-A4DE-185C395CD317}");
    private static final int E_RENDER = 0;
    private static final int E_CONSOLE = 0;
    private static final int DEVICE_STATE_ACTIVE = 0x00000001;
    private static final int STGM_READ = 0;
    private static final int AUDCLNT_SHAREMODE_SHARED = 0;
    private static final int AUDCLNT_STREAMFLAGS_LOOPBACK = 0x00020000;
    private static final int AUDCLNT_BUFFERFLAGS_SILENT = 0x00000002;
    private static final int CLSCTX_ALL = WTypes.CLSCTX_ALL;
    private static final int S_FALSE = 1;
    private static final int CHUNK_BYTES = 1600;
    private static final long REFTIMES_PER_SECOND = 10_000_000L;
    private static final long BUFFER_DURATION_100NS = REFTIMES_PER_SECOND;
    private static final int TARGET_SAMPLE_RATE = 16_000;
    private static final int TARGET_CHANNELS = 1;

    private final AtomicBoolean running = new AtomicBoolean(true);
    private final String deviceId;

    WasapiLoopbackCapture(String deviceId) { this.deviceId = deviceId; }

    static List<AudioSource> listSources() {
        if (!isWindows()) return List.of();
        boolean comInitialized = false;
        Pointer enumerator = null;
        Pointer collection = null;
        try {
            HRESULT init = Ole32.INSTANCE.CoInitializeEx(Pointer.NULL, Ole32.COINIT_MULTITHREADED);
            int initCode = init.intValue();
            if (failed(initCode)) return List.of();
            comInitialized = initCode != S_FALSE;
            enumerator = createEnumerator();
            PointerByReference collectionRef = new PointerByReference();
            check("IMMDeviceEnumerator.EnumAudioEndpoints",
                    invokeInt(enumerator, 3, E_RENDER, DEVICE_STATE_ACTIVE, collectionRef));
            collection = collectionRef.getValue();
            IntByReference countRef = new IntByReference();
            check("IMMDeviceCollection.GetCount", invokeInt(collection, 3, countRef));
            List<AudioSource> sources = new ArrayList<>();
            for (int index = 0; index < countRef.getValue(); index++) {
                PointerByReference deviceRef = new PointerByReference();
                if (failed(invokeInt(collection, 4, index, deviceRef))) continue;
                Pointer device = deviceRef.getValue();
                try {
                    String id = deviceId(device);
                    String name = friendlyName(device);
                    sources.add(AudioSource.wasapiLoopback(name.isBlank() ? compactDeviceId(id) : name, id));
                } finally { release(device); }
            }
            return sources;
        } catch (LineUnavailableException exception) {
            return List.of();
        } finally {
            release(collection); release(enumerator);
            if (comInitialized) Ole32.INSTANCE.CoUninitialize();
        }
    }

    void capture(Consumer<byte[]> chunkConsumer, BiConsumer<byte[], AudioLevel> monitoredChunkConsumer)
            throws LineUnavailableException {
        capture(chunkConsumer, monitoredChunkConsumer, () -> {});
    }

    void capture(Consumer<byte[]> chunkConsumer, BiConsumer<byte[], AudioLevel> monitoredChunkConsumer,
                 Runnable startedCallback) throws LineUnavailableException {
        boolean comInitialized = false;
        Pointer enumerator = null, device = null, audioClient = null, captureClient = null, mixFormatPointer = null;
        try {
            HRESULT init = Ole32.INSTANCE.CoInitializeEx(Pointer.NULL, Ole32.COINIT_MULTITHREADED);
            int initCode = init.intValue();
            if (failed(initCode)) throw unavailable("CoInitializeEx", initCode);
            comInitialized = initCode != S_FALSE;
            enumerator = createEnumerator();
            PointerByReference deviceRef = new PointerByReference();
            if (deviceId == null || deviceId.isBlank()) {
                check("IMMDeviceEnumerator.GetDefaultAudioEndpoint",
                        invokeInt(enumerator, 4, E_RENDER, E_CONSOLE, deviceRef));
            } else {
                check("IMMDeviceEnumerator.GetDevice",
                        invokeInt(enumerator, 5, deviceIdArgument(deviceId), deviceRef));
            }
            device = deviceRef.getValue();
            PointerByReference audioClientRef = new PointerByReference();
            check("IMMDevice.Activate(IAudioClient)",
                    invokeInt(device, 3, IID_IAUDIO_CLIENT, CLSCTX_ALL, Pointer.NULL, audioClientRef));
            audioClient = audioClientRef.getValue();
            PointerByReference mixFormatRef = new PointerByReference();
            check("IAudioClient.GetMixFormat", invokeInt(audioClient, 8, mixFormatRef));
            mixFormatPointer = mixFormatRef.getValue();
            WaveFormat mixFormat = WaveFormat.from(mixFormatPointer);
            PcmConverter converter = new PcmConverter(mixFormat);
            check("IAudioClient.Initialize", invokeInt(audioClient, 3, AUDCLNT_SHAREMODE_SHARED,
                    AUDCLNT_STREAMFLAGS_LOOPBACK, BUFFER_DURATION_100NS, 0L, mixFormatPointer, Pointer.NULL));
            PointerByReference captureClientRef = new PointerByReference();
            check("IAudioClient.GetService(IAudioCaptureClient)",
                    invokeInt(audioClient, 14, IID_IAUDIO_CAPTURE_CLIENT, captureClientRef));
            captureClient = captureClientRef.getValue();
            check("IAudioClient.Start", invokeInt(audioClient, 10));
            startedCallback.run();
            try { captureLoop(captureClient, converter, chunkConsumer, monitoredChunkConsumer); }
            finally { invokeInt(audioClient, 11); }
        } finally {
            if (mixFormatPointer != null) Ole32.INSTANCE.CoTaskMemFree(mixFormatPointer);
            release(captureClient); release(audioClient); release(device); release(enumerator);
            if (comInitialized) Ole32.INSTANCE.CoUninitialize();
        }
    }

    @Override public void close() { running.set(false); }

    private void captureLoop(Pointer captureClient, PcmConverter converter,
                             Consumer<byte[]> chunkConsumer, BiConsumer<byte[], AudioLevel> monitoredChunkConsumer) {
        ByteArrayOutputStream pending = new ByteArrayOutputStream(CHUNK_BYTES * 2);
        IntByReference nextPacketSize = new IntByReference();
        while (running.get() && !Thread.currentThread().isInterrupted()) {
            int hr = invokeInt(captureClient, 5, nextPacketSize);
            if (failed(hr) || nextPacketSize.getValue() == 0) { sleepQuietly(); continue; }
            while (running.get() && nextPacketSize.getValue() > 0) {
                PointerByReference dataRef = new PointerByReference();
                IntByReference framesRef = new IntByReference();
                IntByReference flagsRef = new IntByReference();
                int bufferHr = invokeInt(captureClient, 3, dataRef, framesRef, flagsRef, Pointer.NULL, Pointer.NULL);
                if (failed(bufferHr)) return;
                int frames = framesRef.getValue();
                byte[] converted = converter.convert(dataRef.getValue(), frames,
                        (flagsRef.getValue() & AUDCLNT_BUFFERFLAGS_SILENT) != 0);
                pending.writeBytes(converted);
                invokeInt(captureClient, 4, frames);
                emitChunks(pending, chunkConsumer, monitoredChunkConsumer);
                int packetHr = invokeInt(captureClient, 5, nextPacketSize);
                if (failed(packetHr)) return;
            }
        }
        emitRemainder(pending, chunkConsumer, monitoredChunkConsumer);
    }

    private static void emitChunks(ByteArrayOutputStream pending, Consumer<byte[]> chunkConsumer,
                                    BiConsumer<byte[], AudioLevel> monitoredChunkConsumer) {
        byte[] all = pending.toByteArray();
        int offset = 0;
        while (all.length - offset >= CHUNK_BYTES) {
            byte[] chunk = new byte[CHUNK_BYTES];
            System.arraycopy(all, offset, chunk, 0, chunk.length);
            publish(chunk, chunkConsumer, monitoredChunkConsumer);
            offset += CHUNK_BYTES;
        }
        pending.reset();
        if (offset < all.length) pending.write(all, offset, all.length - offset);
    }

    private static void emitRemainder(ByteArrayOutputStream pending, Consumer<byte[]> chunkConsumer,
                                       BiConsumer<byte[], AudioLevel> monitoredChunkConsumer) {
        byte[] chunk = pending.toByteArray();
        if (chunk.length > 0) publish(chunk, chunkConsumer, monitoredChunkConsumer);
    }

    private static void publish(byte[] chunk, Consumer<byte[]> chunkConsumer,
                                 BiConsumer<byte[], AudioLevel> monitoredChunkConsumer) {
        chunkConsumer.accept(chunk);
        monitoredChunkConsumer.accept(chunk, AudioLevelAnalyzer.analyzePcm16LittleEndian(chunk, chunk.length));
    }

    private static void sleepQuietly() {
        try { Thread.sleep(10); }
        catch (InterruptedException exception) { Thread.currentThread().interrupt(); }
    }

    private static Pointer createEnumerator() throws LineUnavailableException {
        PointerByReference enumeratorRef = new PointerByReference();
        check("CoCreateInstance(IMMDeviceEnumerator)",
                Ole32.INSTANCE.CoCreateInstance(CLSID_MM_DEVICE_ENUMERATOR, Pointer.NULL,
                        CLSCTX_ALL, IID_IMM_DEVICE_ENUMERATOR, enumeratorRef).intValue());
        return enumeratorRef.getValue();
    }

    private static String deviceId(Pointer device) {
        PointerByReference idRef = new PointerByReference();
        if (failed(invokeInt(device, 5, idRef))) return "";
        Pointer pointer = idRef.getValue();
        try { return pointer == null ? "" : pointer.getWideString(0); }
        finally { if (pointer != null) Ole32.INSTANCE.CoTaskMemFree(pointer); }
    }

    private static String friendlyName(Pointer device) {
        Pointer propertyStore = null;
        Variant.VARIANT value = new Variant.VARIANT();
        try {
            PointerByReference storeRef = new PointerByReference();
            if (failed(invokeInt(device, 4, STGM_READ, storeRef))) return "";
            propertyStore = storeRef.getValue();
            Memory key = propertyKeyDeviceFriendlyName();
            if (failed(invokeInt(propertyStore, 5, key, value.getPointer()))) return "";
            value.read();
            Object javaValue = value.getValue();
            return javaValue == null ? "" : javaValue.toString();
        } finally {
            OleAuto.INSTANCE.VariantClear(value);
            release(propertyStore);
        }
    }

    private static Memory propertyKeyDeviceFriendlyName() {
        GUID fmtid = new GUID("{A45C254E-DF1C-4EFD-8020-67D146A850E0}");
        Memory memory = new Memory(20);
        memory.write(0, fmtid.toByteArray(), 0, 16);
        memory.setInt(16, 14);
        return memory;
    }

    private static String compactDeviceId(String id) {
        if (id == null || id.isBlank()) return "WASAPI 输出设备";
        int brace = id.lastIndexOf('{');
        return brace >= 0 ? "WASAPI 输出设备 " + id.substring(brace) : id;
    }

    static WString deviceIdArgument(String deviceId) { return new WString(deviceId); }

    private static int invokeInt(Pointer comObject, int vtableIndex, Object... args) {
        Pointer vtable = comObject.getPointer(0);
        Function function = Function.getFunction(
                vtable.getPointer((long) vtableIndex * Native.POINTER_SIZE), Function.ALT_CONVENTION);
        Object[] fullArgs = new Object[args.length + 1];
        fullArgs[0] = comObject;
        System.arraycopy(args, 0, fullArgs, 1, args.length);
        return (Integer) function.invoke(Integer.class, fullArgs);
    }

    private static void release(Pointer comObject) { if (comObject != null) invokeInt(comObject, 2); }
    private static void check(String operation, int hr) throws LineUnavailableException {
        if (failed(hr)) throw unavailable(operation, hr);
    }
    private static boolean failed(int hr) { return hr < 0; }
    private static LineUnavailableException unavailable(String operation, int hr) {
        return new LineUnavailableException(operation + " failed: 0x" + String.format(Locale.ROOT, "%08X", hr));
    }

    static final class PcmConverter {
        private final WaveFormat format;
        private final double sourceFramesPerTargetFrame;
        private double nextSourceFrame;

        PcmConverter(WaveFormat format) {
            this.format = format;
            this.sourceFramesPerTargetFrame = (double) format.samplesPerSecond / TARGET_SAMPLE_RATE;
        }

        byte[] convert(Pointer data, int sourceFrames, boolean silent) {
            if (sourceFrames <= 0) return new byte[0];
            int targetFrames = (int) Math.floor((sourceFrames - nextSourceFrame) / sourceFramesPerTargetFrame);
            if (targetFrames <= 0) { nextSourceFrame -= sourceFrames; return new byte[0]; }
            byte[] out = new byte[targetFrames * TARGET_CHANNELS * Short.BYTES];
            int outOffset = 0;
            for (int i = 0; i < targetFrames; i++) {
                int frameIndex = (int) Math.floor(nextSourceFrame);
                short sample = silent ? 0 : sampleMono16(data, frameIndex);
                out[outOffset++] = (byte) (sample & 0xff);
                out[outOffset++] = (byte) ((sample >>> 8) & 0xff);
                nextSourceFrame += sourceFramesPerTargetFrame;
            }
            nextSourceFrame -= sourceFrames;
            return out;
        }

        private short sampleMono16(Pointer data, int frameIndex) {
            double sum = 0.0;
            for (int channel = 0; channel < format.channels; channel++) sum += sample(data, frameIndex, channel);
            double mono = sum / format.channels;
            int pcm = (int) Math.round(Math.max(-1.0, Math.min(1.0, mono)) * 32767.0);
            return (short) pcm;
        }

        private double sample(Pointer data, int frameIndex, int channel) {
            int offset = frameIndex * format.blockAlign + channel * format.bytesPerSample();
            if (format.floatSamples()) return data.getFloat(offset);
            if (format.bitsPerSample == 16) return data.getShort(offset) / 32768.0;
            if (format.bitsPerSample == 24) {
                int b0 = data.getByte(offset) & 0xff, b1 = data.getByte(offset + 1) & 0xff, b2 = data.getByte(offset + 2);
                return (b0 | (b1 << 8) | (b2 << 16)) / 8388608.0;
            }
            if (format.bitsPerSample == 32) return data.getInt(offset) / 2147483648.0;
            return 0.0;
        }
    }

    @Structure.FieldOrder({"formatTag","channels","samplesPerSecond","averageBytesPerSecond","blockAlign","bitsPerSample","extraSize"})
    public static final class WaveFormatEx extends Structure {
        public short formatTag, channels, blockAlign, bitsPerSample, extraSize;
        public int samplesPerSecond, averageBytesPerSecond;
        public WaveFormatEx(Pointer pointer) { super(pointer); read(); }
    }

    static final class WaveFormat {
        private static final int WAVE_FORMAT_IEEE_FLOAT = 0x0003;
        private static final int WAVE_FORMAT_EXTENSIBLE = 0xfffe;
        private static final String KSDATAFORMAT_SUBTYPE_IEEE_FLOAT = "00000003-0000-0010-8000-00aa00389b71";
        final int formatTag, channels, samplesPerSecond, blockAlign, bitsPerSample;
        final boolean extensibleFloat;

        private WaveFormat(WaveFormatEx ex, boolean extensibleFloat) {
            this.formatTag = Short.toUnsignedInt(ex.formatTag);
            this.channels = Short.toUnsignedInt(ex.channels);
            this.samplesPerSecond = ex.samplesPerSecond;
            this.blockAlign = Short.toUnsignedInt(ex.blockAlign);
            this.bitsPerSample = Short.toUnsignedInt(ex.bitsPerSample);
            this.extensibleFloat = extensibleFloat;
        }

        static WaveFormat from(Pointer pointer) throws LineUnavailableException {
            WaveFormatEx ex = new WaveFormatEx(pointer);
            WaveFormat format = new WaveFormat(ex, isExtensibleFloat(pointer, ex));
            if (format.channels <= 0 || format.samplesPerSecond <= 0 || format.blockAlign <= 0)
                throw new LineUnavailableException("Unsupported WASAPI mix format");
            if (!format.floatSamples() && format.bitsPerSample != 16 && format.bitsPerSample != 24 && format.bitsPerSample != 32)
                throw new LineUnavailableException("Unsupported WASAPI bit depth: " + format.bitsPerSample);
            return format;
        }

        int bytesPerSample() { return Math.max(1, bitsPerSample / Byte.SIZE); }
        boolean floatSamples() { return formatTag == WAVE_FORMAT_IEEE_FLOAT || extensibleFloat; }

        private static boolean isExtensibleFloat(Pointer pointer, WaveFormatEx ex) {
            if (Short.toUnsignedInt(ex.formatTag) != WAVE_FORMAT_EXTENSIBLE || Short.toUnsignedInt(ex.extraSize) < 22)
                return false;
            byte[] guidBytes = pointer.getByteArray(24, 16);
            return new IID(guidBytes).toGuidString().toLowerCase(Locale.ROOT).equals(KSDATAFORMAT_SUBTYPE_IEEE_FLOAT);
        }
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("windows");
    }
}
