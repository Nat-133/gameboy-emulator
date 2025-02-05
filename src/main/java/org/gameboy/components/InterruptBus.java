package org.gameboy.components;

import org.gameboy.MemoryMapConstants;
import org.gameboy.instructions.targets.Interrupt;
import org.gameboy.utils.BitUtilities;

import java.util.List;

import static org.gameboy.MemoryMapConstants.IF_ADDRESS;
import static org.gameboy.instructions.targets.Interrupt.INTERRUPT_PRIORITY;
import static org.gameboy.utils.BitUtilities.get_bit;
import static org.gameboy.utils.BitUtilities.set_bit;

public class InterruptBus {
    private final Memory memory;

    public InterruptBus(Memory memory) {
        this.memory = memory;
    }

    public List<Interrupt> activeInterrupts() {
        byte interruptByte = BitUtilities.and(memory.read(IF_ADDRESS), memory.read(MemoryMapConstants.IE_ADDRESS));
        return INTERRUPT_PRIORITY.stream()
                .filter(interrupt -> get_bit(interruptByte, interrupt.index()))
                .toList();
    }

    public void deactivateInterrupt(Interrupt interrupt) {
        byte newInterruptFlag = set_bit(memory.read(IF_ADDRESS), interrupt.index(), false);
        memory.write(IF_ADDRESS, newInterruptFlag);
    }
}
