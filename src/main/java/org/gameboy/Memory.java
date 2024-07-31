package org.gameboy;

import static org.gameboy.utils.BitUtilities.uint;

public class Memory {
    private final byte[] memory;

    public Memory() {
        memory = new byte[0xFFFF];
    }

    public short read(short address) {
        int firstAddress = uint(address);
        int lowerByte = uint(memory[firstAddress]);
        int upperByte = uint(firstAddress + 1 < memory.length ? memory[firstAddress + 1] : 0);

        return (short) ((upperByte << 8) + lowerByte);
    }

    public void write(short address, byte value) {
        memory[uint(address)] = value;
    }

    public void write(short address, short value) {
        memory[uint(address)] = (byte) (value & 0x00FF);
        memory[uint(address) + 1] = (byte) (value >> 8);
    }
}
