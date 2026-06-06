package com.fanfan.interpreter.asr;

public interface AsrClient extends AutoCloseable {
    void start(AsrListener listener) throws Exception;
    void appendPcm(byte[] pcmChunk);
    void finish() throws Exception;
    @Override void close();
}
