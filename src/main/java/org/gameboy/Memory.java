package org.gameboy;

import static org.gameboy.utils.BitUtilities.uint;

public class Memory {
    private final byte[] memory;

    public Memory() {
        memory = new byte[0xFFFF+1];
    }

    public byte read(short address) {
        int firstAddress = uint(address);
        return memory[firstAddress];
    }

    public void write(short address, byte value) {
        memory[uint(address)] = value;
    }
}
