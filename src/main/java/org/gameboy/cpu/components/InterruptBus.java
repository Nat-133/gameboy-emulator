package org.gameboy.cpu.components;

import org.gameboy.common.Clock;
import org.gameboy.common.Interrupt;
import org.gameboy.common.Memory;

import java.util.List;

import static org.gameboy.common.MemoryMapConstants.IE_ADDRESS;
import static org.gameboy.common.MemoryMapConstants.IF_ADDRESS;
import static org.gameboy.utils.BitUtilities.*;

public class InterruptBus {
    private static final List<Interrupt> INTERRUPT_PRIORITY = List.of(
            Interrupt.JOYPAD,
            Interrupt.SERIAL,
            Interrupt.TIMER,
            Interrupt.STAT,
            Interrupt.VBLANK
    );

    private final Memory memory;
    private final Clock clock;

    public InterruptBus(Memory memory, Clock clock) {
        this.memory = memory;
        this.clock = clock;
    }

    public boolean hasInterrupts() {
        return 0 != calculateActiveInterruptByte();
    }

    private byte calculateActiveInterruptByte() {
        return and(memory.read(IF_ADDRESS), memory.read(IE_ADDRESS));
    }

    public List<Interrupt> activeInterrupts() {
        byte interruptByte = calculateActiveInterruptByte();
        return INTERRUPT_PRIORITY.stream()
                .filter(interrupt -> get_bit(interruptByte, interrupt.index()))
                .toList();
    }

    public void deactivateInterrupt(Interrupt interrupt) {
        byte newInterruptFlag = set_bit(memory.read(IF_ADDRESS), interrupt.index(), false);
        memory.write(IF_ADDRESS, newInterruptFlag);
    }

    public void waitForInterrupt() {
        while (!hasInterrupts()) {
            clock.tick();
        }
    }
}
