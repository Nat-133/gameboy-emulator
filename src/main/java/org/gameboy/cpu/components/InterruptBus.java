package org.gameboy.cpu.components;

import org.gameboy.common.ByteRegister;
import org.gameboy.common.Clock;
import org.gameboy.common.Interrupt;

import java.util.List;

import static org.gameboy.utils.BitUtilities.*;

public class InterruptBus {
    private static final List<Interrupt> INTERRUPT_PRIORITY = List.of(
            Interrupt.JOYPAD,
            Interrupt.SERIAL,
            Interrupt.TIMER,
            Interrupt.STAT,
            Interrupt.VBLANK
    );

    private final Clock clock;
    private final ByteRegister interruptFlagsRegister;
    private final ByteRegister interruptEnableRegister;

    public InterruptBus(Clock clock, ByteRegister interruptFlagsRegister, ByteRegister interruptEnableRegister) {
        this.clock = clock;
        this.interruptFlagsRegister = interruptFlagsRegister;
        this.interruptEnableRegister = interruptEnableRegister;
    }

    public boolean hasInterrupts() {
        return 0 != calculateActiveInterruptByte();
    }

    private byte calculateActiveInterruptByte() {
        return and(interruptFlagsRegister.read(), interruptEnableRegister.read());
    }

    public List<Interrupt> activeInterrupts() {
        byte interruptByte = calculateActiveInterruptByte();
        return INTERRUPT_PRIORITY.stream()
                .filter(interrupt -> get_bit(interruptByte, interrupt.index()))
                .toList();
    }

    public void deactivateInterrupt(Interrupt interrupt) {
        byte newInterruptFlag = set_bit(interruptFlagsRegister.read(), interrupt.index(), false);
        interruptFlagsRegister.write(newInterruptFlag);
    }

    public void waitForInterrupt() {
        while (!hasInterrupts()) {
            clock.tick();
        }
    }
}
