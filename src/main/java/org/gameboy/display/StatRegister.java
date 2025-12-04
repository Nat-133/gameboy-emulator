package org.gameboy.display;

import org.gameboy.common.ByteRegister;
import org.gameboy.utils.BitUtilities;

public class StatRegister implements ByteRegister {
    private byte value;

    // Read/write mask: bits 3-6 are writable by game code, bits 0-2 are read-only
    private static final int WRITABLE_MASK = 0b0111_1000;
    private static final int READ_ONLY_MASK = 0b0000_0111;
    private static final int MODE_MASK = 0b0000_0011;
    private static final int COINCIDENCE_BIT = 2;

    public StatRegister(byte initialValue) {
        this.value = initialValue;
    }

    public StatRegister(int initialValue) {
        this((byte) initialValue);
    }

    @Override
    public byte read() {
        // Bit 7 always returns 1 when read
        return (byte) (value | 0x80);
    }

    @Override
    public void write(byte newValue) {
        // For external writes: preserve read-only bits (0-2), only update writable bits (3-6)
        // Bit 7 is unused and can be ignored on write
        byte preservedBits = (byte) (value & READ_ONLY_MASK);
        byte writableBits = (byte) (newValue & WRITABLE_MASK);
        this.value = (byte) (preservedBits | writableBits);
    }

    public void setMode(StatParser.PpuMode mode) {
        int modeValue = switch (mode) {
            case H_BLANK -> 0;
            case V_BLANK -> 1;
            case OAM_SCANNING -> 2;
            case DRAWING -> 3;
        };
        this.value = (byte) ((value & ~MODE_MASK) | modeValue);
    }

    public void setCoincidenceFlag(boolean flag) {
        this.value = BitUtilities.set_bit(value, COINCIDENCE_BIT, flag);
    }
}
