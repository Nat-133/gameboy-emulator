package org.gameboy.components;

import org.gameboy.common.ByteRegister;

import java.util.concurrent.atomic.AtomicInteger;

import static org.gameboy.utils.BitUtilities.uint;

public class TacRegister implements ByteRegister {
    private static final int UPPER_BITS_MASK = 0xF8;
    private final AtomicInteger value;
    private WriteListener writeListener = () -> {};

    public TacRegister() {
        this.value = new AtomicInteger(0xF8);
    }

    @Override
    public byte read() {
        return (byte) (value.get() | UPPER_BITS_MASK);
    }

    @Override
    public void write(byte newValue) {
        value.set(uint(newValue));
        writeListener.onWrite();
    }

    public void setWriteListener(WriteListener listener) {
        this.writeListener = listener;
    }

    @FunctionalInterface
    public interface WriteListener {
        void onWrite();
    }
}
