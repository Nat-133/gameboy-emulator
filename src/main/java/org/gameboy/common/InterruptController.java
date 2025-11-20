package org.gameboy.common;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.gameboy.common.annotations.InterruptFlags;

import static org.gameboy.utils.BitUtilities.set_bit;

@Singleton
public class InterruptController {
    private final ByteRegister interruptFlagsRegister;

    @Inject
    public InterruptController(@InterruptFlags ByteRegister interruptFlagsRegister) {
        this.interruptFlagsRegister = interruptFlagsRegister;
    }

    public void setInterrupt(Interrupt interrupt) {
        byte flags = interruptFlagsRegister.read();
        interruptFlagsRegister.write(set_bit(flags, interrupt.index(), true));
    }
}
