package org.gameboy.common;

import java.util.concurrent.atomic.AtomicInteger;

import static org.gameboy.utils.BitUtilities.uint;

public class IntBackedRegister implements ByteRegister {
    private final AtomicInteger value = new AtomicInteger(0);

    @Override
    public byte read() {
        return (byte) value.get();
    }

    @Override
    public void write(byte newValue) {
        value.set(uint(newValue));
    }
}
