package org.gameboy.components;

import org.gameboy.common.ByteRegister;

public class DividerRegister implements ByteRegister {
    private final InternalTimerCounter internalCounter;

    public DividerRegister(InternalTimerCounter internalCounter) {
        this.internalCounter = internalCounter;
    }

    @Override
    public byte read() {
        return (byte) ((internalCounter.getValue() >> 8) & 0xFF);
    }

    @Override
    public void write(byte value) {
        internalCounter.reset();
    }
}
