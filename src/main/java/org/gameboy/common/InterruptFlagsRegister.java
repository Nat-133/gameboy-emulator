package org.gameboy.common;

import java.util.concurrent.atomic.AtomicInteger;

import static org.gameboy.utils.BitUtilities.uint;

public class InterruptFlagsRegister implements ByteRegister {
    private static final int UPPER_BITS_MASK = 0xE0;
    private final AtomicInteger value = new AtomicInteger(0);

    public InterruptFlagsRegister() {
        this.value.set(0);
    }

    @Override
    public byte read() {
        return (byte) (value.get() | UPPER_BITS_MASK);
    }

    @Override
    public void write(byte newValue) {
        value.set(uint(newValue));
    }
}
