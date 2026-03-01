package org.gameboy.audio;

import org.gameboy.common.ByteRegister;

public class ApuRegister implements ByteRegister {
    private final int readMask;
    private int value;
    private WriteCallback writeCallback = v -> {};

    public ApuRegister(int readMask) {
        this(readMask, 0);
    }

    public ApuRegister(int readMask, int initialValue) {
        this.readMask = readMask;
        this.value = initialValue & 0xFF;
    }

    @Override
    public byte read() {
        return (byte) ((value & readMask) | (~readMask & 0xFF));
    }

    @Override
    public void write(byte newValue) {
        value = newValue & 0xFF;
        writeCallback.onWrite(newValue);
    }

    public void setWriteCallback(WriteCallback callback) {
        this.writeCallback = callback;
    }

    public int getRawValue() {
        return value;
    }

    public void setRawValue(int v) {
        this.value = v & 0xFF;
    }

    @FunctionalInterface
    public interface WriteCallback {
        void onWrite(byte value);
    }
}
