package org.gameboy.common;

import static org.gameboy.utils.BitUtilities.set_bit;

public class InterruptController {
    private final Memory memory;

    public InterruptController(Memory memory) {
        this.memory = memory;
    }

    public void setInterrupt(Interrupt interrupt) {
        byte interruptFlags = memory.read(MemoryMapConstants.IF_ADDRESS);
        memory.write(MemoryMapConstants.IF_ADDRESS, set_bit(interruptFlags, interrupt.index(), true));
    }
}
