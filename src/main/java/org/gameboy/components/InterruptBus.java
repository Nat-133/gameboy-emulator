package org.gameboy.components;

import org.gameboy.instructions.targets.Interrupt;

import java.util.List;

import static org.gameboy.MemoryMapConstants.IE_ADDRESS;
import static org.gameboy.MemoryMapConstants.IF_ADDRESS;
import static org.gameboy.instructions.targets.Interrupt.INTERRUPT_PRIORITY;
import static org.gameboy.utils.BitUtilities.*;

public class InterruptBus {
    private final Memory memory;

    public InterruptBus(Memory memory) {
        this.memory = memory;
    }

    public boolean hasInterrupts() {
        return 0 != and(memory.read(IF_ADDRESS), memory.read(IE_ADDRESS));
    }

    public List<Interrupt> activeInterrupts() {
        byte interruptByte = and(memory.read(IF_ADDRESS), memory.read(IE_ADDRESS));
        return INTERRUPT_PRIORITY.stream()
                .filter(interrupt -> get_bit(interruptByte, interrupt.index()))
                .toList();
    }

    public void deactivateInterrupt(Interrupt interrupt) {
        byte newInterruptFlag = set_bit(memory.read(IF_ADDRESS), interrupt.index(), false);
        memory.write(IF_ADDRESS, newInterruptFlag);
    }
}
