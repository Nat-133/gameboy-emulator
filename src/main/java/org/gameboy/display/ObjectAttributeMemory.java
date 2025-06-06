package org.gameboy.display;

import org.gameboy.common.Memory;
import org.gameboy.utils.BitUtilities;

public class ObjectAttributeMemory {
    public static final int SIZE = 0x9f;
    private final short START_ADDRESS = (short) 0xFE00;
    private final Memory memory;

    public ObjectAttributeMemory(Memory memory) {
        // this pulls values from main memory. Really should be the other way around.
        this.memory = memory;
    }

    public short read(int address) {
        if (address > SIZE) {
            return (short) 0;
        }

        byte lower = memory.read((short) (address + START_ADDRESS));
        byte upper = memory.read((short) (address + START_ADDRESS + 1));

        return BitUtilities.concat(upper, lower);
    }
}
