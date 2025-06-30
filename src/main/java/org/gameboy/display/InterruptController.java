package org.gameboy.display;

import org.gameboy.common.Interrupt;
import org.gameboy.common.Memory;
import org.gameboy.common.MemoryMapConstants;

import static org.gameboy.utils.BitUtilities.set_bit;

public class InterruptController {
    private final Memory memory;

    public InterruptController(Memory memory) {
        this.memory = memory;
    }

    public void sendHBLANK() {
        setInterrupt(Interrupt.VBLANK);  // todo
    }

    public void sendVBLANK() {
        setInterrupt(Interrupt.VBLANK);
    }

    public void sendScanlineAtWindow() {
        setInterrupt(Interrupt.STAT);
    }

    private void setInterrupt(Interrupt interrupt) {
        byte interruptFlags = memory.read(MemoryMapConstants.IF_ADDRESS);
        memory.write(MemoryMapConstants.IF_ADDRESS, set_bit(interruptFlags, interrupt.index(), true));
    }
}
