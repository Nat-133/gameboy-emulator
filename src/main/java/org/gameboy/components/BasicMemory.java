package org.gameboy.components;

import static org.gameboy.utils.BitUtilities.uint;

public class BasicMemory implements Memory {
    private final byte[] memory;

    public BasicMemory() {
        memory = new byte[0xFFFF+1];
    }

    @Override
    public byte read(short address) {
        int firstAddress = uint(address);
        return memory[firstAddress];
    }

    @Override
    public void write(short address, byte value) {
        memory[uint(address)] = value;
    }
}
